import { Box, Text } from '@chakra-ui/react';

/**
 * Tooltip content for the dashboard charts: the value first, then what it is
 * for. Recharts passes `active`, `payload` and `label`; `formatLabel` and
 * `formatValue` turn them into text.
 */
const ChartTooltip = ({ active, payload, label, formatLabel, formatValue }) => {
  if (!active || !payload?.length) {
    return null;
  }
  const [item] = payload;
  return (
    <Box
      bg="white"
      borderWidth="1px"
      borderColor="gray.200"
      borderRadius="md"
      px={3}
      py={2}
      boxShadow="md"
    >
      <Text fontWeight="semibold">{formatValue(item.value)}</Text>
      <Text fontSize="sm" color="gray.600">
        {formatLabel(label ?? item.name)}
      </Text>
    </Box>
  );
};

export default ChartTooltip;
