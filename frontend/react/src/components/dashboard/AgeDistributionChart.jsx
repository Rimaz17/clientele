import { usePrefersReducedMotion, useToken } from '@chakra-ui/react';
import { Bar, BarChart, LabelList, ResponsiveContainer, Tooltip, XAxis } from 'recharts';
import ChartCard from './ChartCard.jsx';
import ChartTooltip from './ChartTooltip.jsx';
import { formatCount, plural } from './formatters.js';

// The API's "16-24" reads better with an en dash.
export const formatRange = (range) => range.replace('-', '–');

const formatCustomers = (count) => plural(count, 'customer');

export const ageSummary = (ageDistribution) => {
  const ranges = ageDistribution
    .map(({ range, count }) => `${formatRange(range)}: ${formatCount(count)}`)
    .join(', ');
  const largest = ageDistribution.reduce((best, range) =>
    range.count > best.count ? range : best,
  );
  return largest.count === 0
    ? `No customers in any age range.`
    : `Customers by age range. ${ranges}. The largest group is ${formatRange(largest.range)}.`;
};

/** Customers per age range, youngest first, as a column chart. */
const AgeDistributionChart = ({ ageDistribution = [], isLoading = false }) => {
  const [series, grid, axis] = useToken('colors', ['chart.series1', 'chart.grid', 'chart.axis']);
  const reduceMotion = usePrefersReducedMotion();
  const tick = { fill: axis, fontSize: 12 };

  return (
    <ChartCard
      title="Age distribution"
      description="Customers per age range"
      summary={ageDistribution.length > 0 ? ageSummary(ageDistribution) : 'No age data.'}
      table={{
        columns: ['Age range', 'Customers'],
        rows: ageDistribution.map(({ range, count }) => [formatRange(range), formatCount(count)]),
      }}
      height={220}
      isLoading={isLoading}
    >
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={ageDistribution} margin={{ top: 20, right: 0, bottom: 0, left: 0 }}>
          <XAxis
            dataKey="range"
            tickFormatter={formatRange}
            tick={tick}
            tickLine={false}
            axisLine={{ stroke: grid }}
            interval={0}
          />
          <Tooltip
            cursor={{ fill: grid, fillOpacity: 0.5 }}
            content={<ChartTooltip formatLabel={formatRange} formatValue={formatCustomers} />}
          />
          <Bar
            dataKey="count"
            fill={series}
            radius={[4, 4, 0, 0]}
            maxBarSize={24}
            isAnimationActive={!reduceMotion}
          >
            <LabelList dataKey="count" position="top" fill={axis} fontSize={12} />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  );
};

export default AgeDistributionChart;
