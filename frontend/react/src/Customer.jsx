import { Box, Button, Flex, Heading, Spinner, Stack, Text, Wrap, WrapItem } from '@chakra-ui/react';
import { useEffect } from 'react';
import SidebarWithHeader from './components/shared/SideBar.jsx';
import CardWithImage from './components/customer/CustomerCard.jsx';
import CreateCustomerDrawer from './components/customer/CreateCustomerDrawer.jsx';
import CustomerListToolbar from './components/customer/CustomerListToolbar.jsx';
import CustomerPagination from './components/customer/CustomerPagination.jsx';
import useCustomerListParams from './hooks/useCustomerListParams.js';
import useCustomers from './hooks/useCustomers.js';
import { errorNotification } from './services/notification.js';
import { getApiErrorMessage } from './services/apiErrors.js';

const EmptyState = ({ title, children }) => (
  <Box textAlign="center" py={12} px={4}>
    <Text fontSize="lg" fontWeight="semibold">
      {title}
    </Text>
    {children}
  </Box>
);

const Customer = () => {
  const { params, updateParams, clearFilters, hasFilters } = useCustomerListParams();
  const { data, loading, error, refetch } = useCustomers(params);

  useEffect(() => {
    if (error) {
      errorNotification('Could not load customers', getApiErrorMessage(error));
    }
  }, [error]);

  // A page past the end, for example after deleting the last customer on the
  // last page or from an old link, moves to the new last page.
  useEffect(() => {
    if (!loading && data && data.totalPages > 0 && params.page > data.totalPages) {
      updateParams({ page: data.totalPages }, { replace: true });
    }
  }, [loading, data, params.page, updateParams]);

  const renderList = () => {
    if (!data && loading) {
      return (
        <Flex justify="center" py={12}>
          <Spinner
            thickness="4px"
            speed="0.65s"
            emptyColor="gray.200"
            color="blue.500"
            size="xl"
            label="Loading customers"
          />
        </Flex>
      );
    }
    if (!data) {
      return (
        <EmptyState title="Customers could not be loaded">
          <Text color="gray.600" mt={1}>
            Check your connection and try again.
          </Text>
          <Button mt={4} onClick={refetch}>
            Try again
          </Button>
        </EmptyState>
      );
    }
    if (data.totalElements === 0) {
      return hasFilters ? (
        <EmptyState title="No customers match these filters">
          <Text color="gray.600" mt={1}>
            Try a different search or remove a filter.
          </Text>
          <Button mt={4} onClick={clearFilters}>
            Clear filters
          </Button>
        </EmptyState>
      ) : (
        <EmptyState title="No customers yet">
          <Text color="gray.600" mt={1}>
            Use Create customer to add the first one.
          </Text>
        </EmptyState>
      );
    }
    return (
      <Stack spacing={6}>
        {/* The current page stays visible, dimmed, while the next one loads. */}
        <Box aria-busy={loading} opacity={loading ? 0.6 : 1} transition="opacity 0.2s">
          <Wrap justify="center" spacing="30px">
            {data.content.map((customer) => (
              <WrapItem key={customer.id}>
                <CardWithImage {...customer} fetchCustomers={refetch} />
              </WrapItem>
            ))}
          </Wrap>
        </Box>
        <CustomerPagination
          page={params.page}
          size={params.size}
          totalElements={data.totalElements}
          totalPages={data.totalPages}
          onPageChange={(page) => updateParams({ page })}
          onSizeChange={(size) => updateParams({ size })}
        />
      </Stack>
    );
  };

  return (
    <SidebarWithHeader>
      <Stack spacing={6}>
        <Flex justify="space-between" align="center" wrap="wrap" gap={4}>
          <Heading as="h1" size="lg">
            Customers
          </Heading>
          <CreateCustomerDrawer fetchCustomers={refetch} />
        </Flex>
        <CustomerListToolbar params={params} onChange={updateParams} />
        {renderList()}
      </Stack>
    </SidebarWithHeader>
  );
};

export default Customer;
