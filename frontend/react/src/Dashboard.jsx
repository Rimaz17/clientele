import { Button, Card, CardBody, Heading, SimpleGrid, Stack, Text } from '@chakra-ui/react';
import { Link as RouterLink } from 'react-router-dom';
import SidebarWithHeader from './components/shared/SideBar.jsx';
import StatCard from './components/dashboard/StatCard.jsx';
import SignupsChart from './components/dashboard/SignupsChart.jsx';
import GenderSplitChart from './components/dashboard/GenderSplitChart.jsx';
import AgeDistributionChart from './components/dashboard/AgeDistributionChart.jsx';
import { formatAverage, formatCount, formatPercent } from './components/dashboard/formatters.js';
import useCustomerStats from './hooks/useCustomerStats.js';
import { getApiErrorMessage } from './services/apiErrors.js';

const Panel = ({ title, children, ...rest }) => (
  <Card variant="outline" {...rest}>
    <CardBody textAlign="center" py={12}>
      <Text fontSize="lg" fontWeight="semibold">
        {title}
      </Text>
      {children}
    </CardBody>
  </Card>
);

const StatCards = ({ stats, isLoading }) => (
  <SimpleGrid columns={{ base: 1, md: 3 }} spacing={4}>
    <StatCard
      label="Total customers"
      value={stats && formatCount(stats.totalCustomers)}
      detail="Every account, admins included"
      isLoading={isLoading}
    />
    <StatCard
      label="New in the last 30 days"
      value={stats && formatCount(stats.newLast30Days)}
      detail={
        stats && stats.totalCustomers > 0
          ? `${formatPercent(stats.newLast30Days, stats.totalCustomers)} of all customers`
          : 'No customers yet'
      }
      isLoading={isLoading}
    />
    <StatCard
      label="Average age"
      value={stats && formatAverage(stats.averageAge)}
      detail={stats?.averageAge === null ? 'No customers yet' : 'Years, across all customers'}
      isLoading={isLoading}
    />
  </SimpleGrid>
);

const Charts = ({ stats, isLoading }) => (
  <Stack spacing={4}>
    <SignupsChart signups={stats?.signupsByMonth} isLoading={isLoading} />
    <SimpleGrid columns={{ base: 1, lg: 2 }} spacing={4}>
      <GenderSplitChart genderSplit={stats?.genderSplit} isLoading={isLoading} />
      <AgeDistributionChart ageDistribution={stats?.ageDistribution} isLoading={isLoading} />
    </SimpleGrid>
  </Stack>
);

const Dashboard = () => {
  const { data, loading, error, refetch } = useCustomerStats();
  const isLoading = !data && loading;

  const renderContent = () => {
    if (!data && !loading) {
      return (
        <Panel title="Statistics could not be loaded" role="alert">
          <Text color="gray.600" mt={1}>
            {getApiErrorMessage(error, 'Check your connection and try again.')}
          </Text>
          <Button mt={4} onClick={refetch}>
            Try again
          </Button>
        </Panel>
      );
    }
    return (
      <>
        <StatCards stats={data} isLoading={isLoading} />
        {data?.totalCustomers === 0 ? (
          <Panel title="No customers yet">
            <Text color="gray.600" mt={1}>
              The charts appear once customers sign up.
            </Text>
            <Button as={RouterLink} to="/dashboard/customers" mt={4}>
              Go to customers
            </Button>
          </Panel>
        ) : (
          <Charts stats={data} isLoading={isLoading} />
        )}
      </>
    );
  };

  return (
    <SidebarWithHeader>
      <Stack spacing={6} aria-busy={loading}>
        <Stack spacing={1}>
          <Heading as="h1" size="lg">
            Dashboard
          </Heading>
          <Text color="gray.600">A summary of every customer record.</Text>
        </Stack>
        {renderContent()}
      </Stack>
    </SidebarWithHeader>
  );
};

export default Dashboard;
