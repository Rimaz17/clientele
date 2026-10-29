import { ChakraProvider } from '@chakra-ui/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import CustomerPagination, { pageItems } from './CustomerPagination.jsx';

const renderPagination = (props) => {
  const handlers = { onPageChange: vi.fn(), onSizeChange: vi.fn() };
  render(
    <ChakraProvider>
      <CustomerPagination size={20} totalElements={132} totalPages={7} {...handlers} {...props} />
    </ChakraProvider>,
  );
  return handlers;
};

describe('pageItems', () => {
  it('lists every page when there are seven or fewer', () => {
    expect(pageItems(1, 1)).toEqual([1]);
    expect(pageItems(3, 7)).toEqual([1, 2, 3, 4, 5, 6, 7]);
  });

  it('shows the pages around the current one with gaps', () => {
    expect(pageItems(1, 20)).toEqual([1, 2, 3, 4, 5, 'gap', 20]);
    expect(pageItems(10, 20)).toEqual([1, 'gap', 9, 10, 11, 'gap', 20]);
    expect(pageItems(20, 20)).toEqual([1, 'gap', 16, 17, 18, 19, 20]);
  });
});

describe('CustomerPagination', () => {
  it('summarises which customers are shown', () => {
    renderPagination({ page: 2 });

    expect(screen.getByText('Showing 21–40 of 132')).toBeInTheDocument();
  });

  it('summarises the last, partly filled page', () => {
    renderPagination({ page: 7 });

    expect(screen.getByText('Showing 121–132 of 132')).toBeInTheDocument();
  });

  // jsdom applies no media queries, so the numbered buttons keep their
  // mobile display: none and must be queried as hidden elements.
  it('requests the page that is clicked and marks the current one', async () => {
    const { onPageChange } = renderPagination({ page: 2 });

    await userEvent.click(screen.getByRole('button', { name: 'Page 3', hidden: true }));

    expect(onPageChange).toHaveBeenCalledWith(3);
    expect(screen.getByRole('button', { name: 'Page 2', hidden: true })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('requests the previous and next pages', async () => {
    const { onPageChange } = renderPagination({ page: 4 });

    await userEvent.click(screen.getByRole('button', { name: 'Previous' }));
    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    expect(onPageChange).toHaveBeenNthCalledWith(1, 3);
    expect(onPageChange).toHaveBeenNthCalledWith(2, 5);
  });

  it('disables Previous on the first page and Next on the last page', () => {
    renderPagination({ page: 1, totalElements: 15, totalPages: 1 });

    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('reports a new page size', async () => {
    const { onSizeChange } = renderPagination({ page: 1 });

    await userEvent.selectOptions(screen.getByLabelText('Per page'), '50');

    expect(onSizeChange).toHaveBeenCalledWith(50);
  });
});
