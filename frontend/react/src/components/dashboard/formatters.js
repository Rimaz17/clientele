const countFormat = new Intl.NumberFormat('en');
const decimalFormat = new Intl.NumberFormat('en', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});
const percentFormat = new Intl.NumberFormat('en', { style: 'percent' });
// The API's months are UTC calendar months, so they are formatted in UTC too.
const shortMonthFormat = new Intl.DateTimeFormat('en', { month: 'short', timeZone: 'UTC' });
const longMonthFormat = new Intl.DateTimeFormat('en', {
  month: 'long',
  year: 'numeric',
  timeZone: 'UTC',
});

// "2026-03" -> the first instant of March 2026 in UTC.
const monthStart = (month) => {
  const [year, monthNumber] = month.split('-').map(Number);
  return new Date(Date.UTC(year, monthNumber - 1, 1));
};

export const formatCount = (value) => countFormat.format(value);

export const formatAverage = (value) => (value === null ? '–' : decimalFormat.format(value));

export const formatPercent = (part, whole) =>
  whole === 0 ? '0%' : percentFormat.format(part / whole);

export const formatShortMonth = (month) => shortMonthFormat.format(monthStart(month));

export const formatLongMonth = (month) => longMonthFormat.format(monthStart(month));

// "1 sign-up", "3 sign-ups"
export const plural = (count, singular, pluralForm = `${singular}s`) =>
  `${formatCount(count)} ${count === 1 ? singular : pluralForm}`;
