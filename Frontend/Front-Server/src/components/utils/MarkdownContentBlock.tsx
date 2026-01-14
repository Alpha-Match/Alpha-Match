import React from 'react';
import ReactMarkdown from 'react-markdown';

interface MarkdownContentBlockProps {
  title: string;
  content: string | null | undefined;
  className?: string; // Optional class for the outer div
}

export const MarkdownContentBlock: React.FC<MarkdownContentBlockProps> = ({ title, content, className }) => {
  if (!content) {
    return null;
  }

  return (
    <div className={className}>
      <h3 className="text-lg font-semibold mb-3 text-text-primary">{title}</h3>
      <div className="prose prose-invert max-w-none text-text-secondary whitespace-pre-wrap leading-relaxed">
        <ReactMarkdown>
          {content}
        </ReactMarkdown>
      </div>
    </div>
  );
};