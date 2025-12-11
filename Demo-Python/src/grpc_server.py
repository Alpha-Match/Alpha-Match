"""
gRPC Streaming Server for Embedding data.

This server receives requests from the Batch Server and streams
embedding data from .pkl files in chunks.

Service: EmbeddingStreamService
Method: StreamEmbedding (Server Streaming RPC)

Port: 50051
"""

import grpc
from concurrent import futures
import logging
import signal
import sys
from typing import Optional

# Import generated protobuf files
from proto import embedding_stream_pb2
from proto import embedding_stream_pb2_grpc

# Import our modules
from config import server_config, data_config, validate_config
from data_loader import load_data_optimized, filter_from_checkpoint
from chunker import chunk_dataframe, chunk_to_rows

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s [%(name)s] - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)


class EmbeddingStreamServicer(embedding_stream_pb2_grpc.EmbeddingStreamServiceServicer):
    """
    gRPC Servicer for streaming embedding data.

    Implements the EmbeddingStreamService defined in embedding_stream.proto.
    """

    def __init__(self):
        """Initialize the servicer."""
        logger.info("Initializing EmbeddingStreamServicer")
        self.data_cache: Optional[any] = None
        self.cache_enabled = False  # Set to True to cache data in memory

    def StreamEmbedding(self, request, context):
        """
        Stream embedding data to client.

        Args:
            request: StreamEmbeddingRequest containing:
                - last_processed_uuid: Checkpoint UUID to resume from
                - chunk_size: Desired chunk size (default: 300)
            context: gRPC context

        Yields:
            RowChunk: Chunks of RecruitRow data

        Raises:
            grpc.StatusCode.INVALID_ARGUMENT: Invalid request parameters
            grpc.StatusCode.INTERNAL: Server error during processing
        """
        try:
            # Extract request parameters
            last_uuid = request.last_processed_uuid or None
            chunk_size = request.chunk_size or data_config.DEFAULT_CHUNK_SIZE

            logger.info("=" * 80)
            logger.info("Received StreamEmbedding request")
            logger.info(f"  Last processed UUID: {last_uuid or 'None (start from beginning)'}")
            logger.info(f"  Requested chunk size: {chunk_size}")
            logger.info("=" * 80)

            # Validate chunk size
            if chunk_size < data_config.MIN_CHUNK_SIZE or chunk_size > data_config.MAX_CHUNK_SIZE:
                error_msg = (
                    f"Invalid chunk_size: {chunk_size}. "
                    f"Must be between {data_config.MIN_CHUNK_SIZE} and {data_config.MAX_CHUNK_SIZE}"
                )
                logger.error(error_msg)
                context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
                context.set_details(error_msg)
                return

            # Load data (optimized)
            logger.info("Loading data...")
            df = load_data_optimized()

            # Filter from checkpoint if provided
            if last_uuid:
                logger.info(f"Filtering from checkpoint: {last_uuid}")
                df = filter_from_checkpoint(df, last_uuid)

            if len(df) == 0:
                logger.warning("No data to stream (all data already processed or empty dataset)")
                return

            logger.info(f"Starting to stream {len(df):,} rows in chunks of {chunk_size}")

            # Stream chunks
            chunk_count = 0
            row_count = 0

            for chunk_df in chunk_dataframe(df, chunk_size):
                # Check if client cancelled
                if context.is_active() is False:
                    logger.warning("Client cancelled the request")
                    break

                chunk_count += 1

                # Convert chunk to row dictionaries
                rows_data = chunk_to_rows(chunk_df)
                row_count += len(rows_data)

                # Create protobuf RecruitRow messages
                proto_rows = []
                for row_dict in rows_data:
                    recruit_row = embedding_stream_pb2.RecruitRow(
                        id=row_dict['id'],
                        company_name=row_dict['company_name'],
                        exp_years=row_dict['exp_years'],
                        english_level=row_dict['english_level'],
                        primary_keyword=row_dict['primary_keyword'],
                        vector=row_dict['vector']
                    )
                    proto_rows.append(recruit_row)

                # Create and yield RowChunk
                row_chunk = embedding_stream_pb2.RowChunk(rows=proto_rows)

                yield row_chunk

                # Log progress
                if chunk_count % data_config.LOG_CHUNK_INTERVAL == 0:
                    progress = (row_count / len(df)) * 100
                    logger.info(
                        f"Streamed chunk {chunk_count} "
                        f"({row_count:,}/{len(df):,} rows, {progress:.1f}%)"
                    )

            # Final log
            logger.info("=" * 80)
            logger.info(f"Streaming completed successfully")
            logger.info(f"  Total chunks sent: {chunk_count}")
            logger.info(f"  Total rows sent: {row_count:,}")
            logger.info("=" * 80)

        except FileNotFoundError as e:
            error_msg = f"Data file not found: {e}"
            logger.error(error_msg)
            context.set_code(grpc.StatusCode.NOT_FOUND)
            context.set_details(error_msg)

        except Exception as e:
            error_msg = f"Internal server error: {type(e).__name__}: {e}"
            logger.error(error_msg, exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(error_msg)


def serve():
    """
    Start the gRPC server.

    The server listens on port 50051 and handles graceful shutdown.
    """
    # Validate configuration
    try:
        validate_config()
    except Exception as e:
        logger.error(f"Configuration validation failed: {e}")
        sys.exit(1)

    # Create gRPC server
    server = grpc.server(
        futures.ThreadPoolExecutor(max_workers=server_config.MAX_WORKERS),
        options=[
            ('grpc.max_send_message_length', server_config.MAX_MESSAGE_LENGTH),
            ('grpc.max_receive_message_length', server_config.MAX_MESSAGE_LENGTH),
        ]
    )

    # Add servicer
    embedding_stream_pb2_grpc.add_EmbeddingStreamServiceServicer_to_server(
        EmbeddingStreamServicer(), server
    )

    # Bind port
    server.add_insecure_port(server_config.HOST)

    # Start server
    server.start()

    logger.info("=" * 80)
    logger.info("🚀 gRPC Server Started Successfully")
    logger.info(f"   Listening on: {server_config.HOST}")
    logger.info(f"   Max workers: {server_config.MAX_WORKERS}")
    logger.info(f"   Max message size: {server_config.MAX_MESSAGE_LENGTH / 1024**2:.0f} MB")
    logger.info(f"   Data file: {data_config.PKL_FILE}")
    logger.info("=" * 80)
    logger.info("Press Ctrl+C to stop the server")

    # Graceful shutdown handler
    def signal_handler(sig, frame):
        logger.info("\nReceived shutdown signal, stopping server...")
        server.stop(grace=5)
        logger.info("Server stopped gracefully")
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    # Wait for termination
    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("\nKeyboard interrupt received, stopping server...")
        server.stop(grace=5)


if __name__ == '__main__':
    logger.info("Starting Demo-Python gRPC Embedding Server")
    serve()
