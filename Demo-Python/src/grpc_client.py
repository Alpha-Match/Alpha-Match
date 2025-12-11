"""
gRPC Test Client for Embedding Stream.

This client tests the EmbeddingStreamService by requesting data
and consuming the stream.

Usage:
    python grpc_client.py [--checkpoint UUID] [--chunk-size SIZE]
"""

import grpc
import argparse
import logging
from typing import Optional

from proto import embedding_stream_pb2
from proto import embedding_stream_pb2_grpc

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)


def test_stream(
    server_address: str = 'localhost:50051',
    last_uuid: Optional[str] = None,
    chunk_size: int = 300
):
    """
    Test streaming from gRPC server.

    Args:
        server_address: Server address (default: localhost:50051)
        last_uuid: Checkpoint UUID to resume from
        chunk_size: Requested chunk size
    """
    logger.info("=" * 80)
    logger.info("gRPC Client Test - Embedding Stream")
    logger.info(f"  Server: {server_address}")
    logger.info(f"  Checkpoint UUID: {last_uuid or 'None (from beginning)'}")
    logger.info(f"  Chunk size: {chunk_size}")
    logger.info("=" * 80)

    # Create channel
    channel = grpc.insecure_channel(server_address)

    try:
        # Create stub
        stub = embedding_stream_pb2_grpc.EmbeddingStreamServiceStub(channel)

        # Create request
        request = embedding_stream_pb2.StreamEmbeddingRequest(
            last_processed_uuid=last_uuid or '',
            chunk_size=chunk_size
        )

        logger.info("Sending request to server...")

        # Call streaming RPC
        chunk_count = 0
        row_count = 0
        first_chunk = None
        last_chunk = None

        try:
            for row_chunk in stub.StreamEmbedding(request):
                chunk_count += 1
                rows_in_chunk = len(row_chunk.rows)
                row_count += rows_in_chunk

                # Save first and last chunk for inspection
                if chunk_count == 1:
                    first_chunk = row_chunk

                last_chunk = row_chunk

                # Log progress
                if chunk_count % 10 == 0:
                    logger.info(f"Received {chunk_count} chunks ({row_count:,} rows)")

            # Final summary
            logger.info("=" * 80)
            logger.info("Stream completed successfully!")
            logger.info(f"  Total chunks received: {chunk_count}")
            logger.info(f"  Total rows received: {row_count:,}")

            # Display first chunk details
            if first_chunk and len(first_chunk.rows) > 0:
                first_row = first_chunk.rows[0]
                logger.info("\nFirst row details:")
                logger.info(f"  ID: {first_row.id}")
                logger.info(f"  Company: {first_row.company_name}")
                logger.info(f"  Exp Years: {first_row.exp_years}")
                logger.info(f"  English Level: {first_row.english_level}")
                logger.info(f"  Primary Keyword: {first_row.primary_keyword}")
                logger.info(f"  Vector dimension: {len(first_row.vector)}")
                logger.info(f"  Vector sample: {list(first_row.vector[:5])}")

            # Display last chunk details
            if last_chunk and len(last_chunk.rows) > 0:
                last_row = last_chunk.rows[-1]
                logger.info("\nLast row details:")
                logger.info(f"  ID: {last_row.id}")
                logger.info(f"  Company: {last_row.company_name}")

            logger.info("=" * 80)

        except grpc.RpcError as e:
            logger.error(f"RPC failed: {e.code()} - {e.details()}")
            raise

    finally:
        channel.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Test gRPC Embedding Stream Client')
    parser.add_argument(
        '--server',
        default='localhost:50051',
        help='Server address (default: localhost:50051)'
    )
    parser.add_argument(
        '--checkpoint',
        help='Last processed UUID to resume from'
    )
    parser.add_argument(
        '--chunk-size',
        type=int,
        default=300,
        help='Chunk size (default: 300)'
    )

    args = parser.parse_args()

    try:
        test_stream(
            server_address=args.server,
            last_uuid=args.checkpoint,
            chunk_size=args.chunk_size
        )
    except Exception as e:
        logger.error(f"Test failed: {e}", exc_info=True)
        exit(1)
