import { Button, Flex, FormControl, FormLabel, HStack, Select, Text } from '@chakra-ui/react';
import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { PAGE_SIZES } from '../../hooks/useCustomerListParams.js';

const GAP = 'gap';

// Page numbers to show: always the first and last page and the pages around
// the current one, with a gap marker where pages are skipped.
export const pageItems = (current, totalPages) => {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }
  const start = Math.max(2, Math.min(current - 1, totalPages - 4));
  const end = Math.min(totalPages - 1, Math.max(current + 1, 5));
  const middle = Array.from({ length: end - start + 1 }, (_, index) => start + index);
  return [
    1,
    ...(start > 2 ? [GAP] : []),
    ...middle,
    ...(end < totalPages - 1 ? [GAP] : []),
    totalPages,
  ];
};

const CustomerPagination = ({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  onSizeChange,
}) => {
  const from = totalElements === 0 ? 0 : (page - 1) * size + 1;
  const to = Math.min(page * size, totalElements);

  return (
    <Flex wrap="wrap" gap={4} align="center" justify="space-between">
      <Text aria-live="polite" color="gray.700" fontSize="sm">
        Showing {from}–{to} of {totalElements}
      </Text>

      <Flex as="nav" aria-label="Pagination" align="center" gap={2}>
        <Button
          size="sm"
          variant="outline"
          bg="white"
          leftIcon={<FiChevronLeft aria-hidden="true" />}
          isDisabled={page <= 1}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </Button>

        {/* Below 480px the numbered buttons do not fit, so the position is text. */}
        <HStack spacing={1} display={{ base: 'none', sm: 'flex' }}>
          {pageItems(page, totalPages).map((item, index) =>
            item === GAP ? (
              <Text key={`gap-${index}`} px={1} color="gray.600" aria-hidden="true">
                …
              </Text>
            ) : (
              <Button
                key={item}
                size="sm"
                minW={9}
                variant={item === page ? 'solid' : 'ghost'}
                colorScheme={item === page ? 'teal' : 'gray'}
                aria-label={`Page ${item}`}
                aria-current={item === page ? 'page' : undefined}
                onClick={() => onPageChange(item)}
              >
                {item}
              </Button>
            ),
          )}
        </HStack>
        <Text display={{ base: 'block', sm: 'none' }} fontSize="sm" color="gray.700">
          Page {page} of {totalPages}
        </Text>

        <Button
          size="sm"
          variant="outline"
          bg="white"
          rightIcon={<FiChevronRight aria-hidden="true" />}
          isDisabled={page >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </Button>
      </Flex>

      <FormControl id="customer-page-size" display="flex" alignItems="center" w="auto">
        <FormLabel fontSize="sm" mb={0} mr={2} whiteSpace="nowrap">
          Per page
        </FormLabel>
        <Select
          size="sm"
          w="5rem"
          bg="white"
          value={size}
          onChange={(event) => onSizeChange(Number(event.target.value))}
        >
          {PAGE_SIZES.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </Select>
      </FormControl>
    </Flex>
  );
};

export default CustomerPagination;
