import {
  Box,
  Flex,
  HStack,
  Stack,
  Text,
  usePrefersReducedMotion,
  useToken,
} from '@chakra-ui/react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import ChartCard from './ChartCard.jsx';
import ChartTooltip from './ChartTooltip.jsx';
import { formatCount, formatPercent, plural } from './formatters.js';

// Each gender keeps its colour whatever the counts are.
const GENDERS = [
  { key: 'MALE', label: 'Male', color: 'chart.series1' },
  { key: 'FEMALE', label: 'Female', color: 'chart.series2' },
];

const formatCustomers = (count) => plural(count, 'customer');

export const genderRows = (genderSplit) => {
  const total = GENDERS.reduce((sum, { key }) => sum + (genderSplit[key] ?? 0), 0);
  return GENDERS.map(({ key, label, color }) => {
    const count = genderSplit[key] ?? 0;
    return { key, label, color, count, percent: formatPercent(count, total) };
  });
};

export const genderSummary = (rows) =>
  `${rows
    .map(({ label, count, percent }) => `${formatCount(count)} ${label.toLowerCase()} (${percent})`)
    .join(' and ')}.`;

/** The split of customers by gender, as a donut with a labelled legend. */
const GenderSplitChart = ({ genderSplit = {}, isLoading = false }) => {
  const colors = useToken(
    'colors',
    GENDERS.map(({ color }) => color),
  );
  const reduceMotion = usePrefersReducedMotion();
  const rows = genderRows(genderSplit);
  const data = rows.map((row, index) => ({ ...row, fill: colors[index] }));

  return (
    <ChartCard
      title="Gender split"
      description="All customers"
      summary={genderSummary(rows)}
      table={{
        columns: ['Gender', 'Customers'],
        rows: rows.map(({ label, count, percent }) => [
          label,
          `${formatCount(count)} (${percent})`,
        ]),
      }}
      height={220}
      isLoading={isLoading}
    >
      <Flex h="100%" align="center" gap={6}>
        <Box flex="1" minW={0} h="100%">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                dataKey="count"
                nameKey="label"
                innerRadius="62%"
                outerRadius="92%"
                startAngle={90}
                endAngle={-270}
                stroke="white"
                strokeWidth={2}
                isAnimationActive={!reduceMotion}
              >
                {data.map(({ key, fill }) => (
                  <Cell key={key} fill={fill} />
                ))}
              </Pie>
              <Tooltip
                content={
                  <ChartTooltip formatLabel={(label) => label} formatValue={formatCustomers} />
                }
              />
            </PieChart>
          </ResponsiveContainer>
        </Box>
        <Stack spacing={3} flexShrink={0}>
          {data.map(({ key, label, count, percent, fill }) => (
            <HStack key={key} spacing={3} align="baseline">
              <Box w="10px" h="10px" borderRadius="sm" bg={fill} flexShrink={0} />
              <Box>
                <Text fontSize="sm" color="gray.600">
                  {label}
                </Text>
                <Text fontWeight="semibold">
                  {formatCount(count)}{' '}
                  <Text as="span" fontWeight="normal" color="gray.600">
                    {percent}
                  </Text>
                </Text>
              </Box>
            </HStack>
          ))}
        </Stack>
      </Flex>
    </ChartCard>
  );
};

export default GenderSplitChart;
