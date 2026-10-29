import { usePrefersReducedMotion, useToken } from '@chakra-ui/react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import ChartCard from './ChartCard.jsx';
import ChartTooltip from './ChartTooltip.jsx';
import { formatCount, formatLongMonth, formatShortMonth, plural } from './formatters.js';

const formatSignups = (count) => plural(count, 'sign-up');

// One sentence for screen readers: the period, the total and the busiest month.
export const signupsSummary = (signups) => {
  if (signups.length === 0) {
    return 'No sign-up data.';
  }
  const period = `${formatLongMonth(signups[0].month)} to ${formatLongMonth(signups.at(-1).month)}`;
  const total = signups.reduce((sum, { count }) => sum + count, 0);
  if (total === 0) {
    return `No sign-ups from ${period}.`;
  }
  const busiest = signups.reduce((best, month) => (month.count > best.count ? month : best));
  return (
    `${formatSignups(total)} from ${period}. ` +
    `The busiest month was ${formatLongMonth(busiest.month)} with ${formatSignups(busiest.count)}.`
  );
};

/** Sign-ups per UTC month over the last 12 months, as an area chart. */
const SignupsChart = ({ signups = [], isLoading = false }) => {
  const [series, grid, axis] = useToken('colors', ['chart.series1', 'chart.grid', 'chart.axis']);
  const reduceMotion = usePrefersReducedMotion();
  const tick = { fill: axis, fontSize: 12 };

  return (
    <ChartCard
      title="Sign-ups per month"
      description={
        signups.length > 0
          ? `${formatLongMonth(signups[0].month)} to ${formatLongMonth(signups.at(-1).month)}`
          : 'Last 12 months'
      }
      summary={signupsSummary(signups)}
      table={{
        columns: ['Month', 'Sign-ups'],
        rows: signups.map(({ month, count }) => [formatLongMonth(month), formatCount(count)]),
      }}
      isLoading={isLoading}
    >
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={signups} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <CartesianGrid vertical={false} stroke={grid} />
          <XAxis
            dataKey="month"
            tickFormatter={formatShortMonth}
            tick={tick}
            tickLine={false}
            axisLine={{ stroke: grid }}
            interval="preserveStartEnd"
            minTickGap={8}
          />
          <YAxis allowDecimals={false} tick={tick} tickLine={false} axisLine={false} width={32} />
          <Tooltip
            cursor={{ stroke: axis, strokeWidth: 1 }}
            content={<ChartTooltip formatLabel={formatLongMonth} formatValue={formatSignups} />}
          />
          <Area
            type="linear"
            dataKey="count"
            stroke={series}
            strokeWidth={2}
            fill={series}
            fillOpacity={0.1}
            dot={{ r: 4, fill: series, stroke: 'white', strokeWidth: 2 }}
            activeDot={{ r: 5, fill: series, stroke: 'white', strokeWidth: 2 }}
            isAnimationActive={!reduceMotion}
          />
        </AreaChart>
      </ResponsiveContainer>
    </ChartCard>
  );
};

export default SignupsChart;
