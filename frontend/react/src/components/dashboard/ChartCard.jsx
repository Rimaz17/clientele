import {
  Box,
  Button,
  Card,
  CardBody,
  Flex,
  Heading,
  Skeleton,
  Table,
  TableContainer,
  Tbody,
  Td,
  Text,
  Th,
  Thead,
  Tr,
} from '@chakra-ui/react';
import { useId, useState } from 'react';

/**
 * A titled dashboard chart with a table view of the same numbers. The chart is
 * one image to screen readers, described by `summary`; the table gives every
 * value without hovering.
 *
 * `table` is `{ columns: [labelHeading, valueHeading], rows: [[label, value], ...] }`.
 */
const ChartCard = ({
  title,
  description,
  summary,
  table,
  height = 260,
  isLoading = false,
  children,
}) => {
  const [showTable, setShowTable] = useState(false);
  const titleId = useId();
  const viewId = useId();

  const renderView = () => {
    if (isLoading) {
      return <Skeleton h={`${height}px`} borderRadius="md" />;
    }
    if (showTable) {
      return (
        <TableContainer>
          <Table size="sm" aria-labelledby={titleId}>
            <Thead>
              <Tr>
                <Th>{table.columns[0]}</Th>
                <Th isNumeric>{table.columns[1]}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {table.rows.map(([label, value]) => (
                <Tr key={label}>
                  <Td>{label}</Td>
                  <Td isNumeric sx={{ fontVariantNumeric: 'tabular-nums' }}>
                    {value}
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        </TableContainer>
      );
    }
    return (
      <Box role="img" aria-label={summary} h={`${height}px`}>
        {children}
      </Box>
    );
  };

  return (
    <Card variant="outline" as="section" aria-labelledby={titleId}>
      <CardBody>
        <Flex justify="space-between" align="flex-start" gap={4} mb={4}>
          <Box>
            <Heading as="h2" size="sm" id={titleId}>
              {title}
            </Heading>
            {description && (
              <Text fontSize="sm" color="gray.600" mt={1}>
                {description}
              </Text>
            )}
          </Box>
          <Button
            size="sm"
            variant="ghost"
            flexShrink={0}
            aria-controls={viewId}
            isDisabled={isLoading}
            onClick={() => setShowTable((current) => !current)}
          >
            {showTable ? 'Show chart' : 'Show table'}
          </Button>
        </Flex>
        <Box id={viewId}>{renderView()}</Box>
      </CardBody>
    </Card>
  );
};

export default ChartCard;
