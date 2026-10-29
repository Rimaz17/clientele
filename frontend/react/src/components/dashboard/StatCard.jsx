import {
  Card,
  CardBody,
  Skeleton,
  Stat,
  StatHelpText,
  StatLabel,
  StatNumber,
} from '@chakra-ui/react';

/**
 * One headline number on the dashboard, with a label and a line of context.
 * While loading, a skeleton holds the value's place so the layout does not jump.
 */
const StatCard = ({ label, value, detail, isLoading = false }) => (
  <Card variant="outline">
    <CardBody>
      <Stat>
        <StatLabel fontSize="sm" color="gray.600">
          {label}
        </StatLabel>
        <Skeleton isLoaded={!isLoading} mt={1} minH="2.5rem" maxW={isLoading ? '6rem' : 'none'}>
          <StatNumber fontSize="3xl" fontWeight="semibold" lineHeight="short">
            {value}
          </StatNumber>
        </Skeleton>
        <StatHelpText mb={0} mt={1} color="gray.600">
          {isLoading ? ' ' : detail}
        </StatHelpText>
      </Stat>
    </CardBody>
  </Card>
);

export default StatCard;
