export type SummaryFact = {
  label: string
  value: string | number
}

type Props = {
  items: SummaryFact[]
  ariaLabel: string
  className?: string
}

export function SummaryFacts({ items, ariaLabel, className }: Props) {
  return (
    <dl className={['summary-facts', className].filter(Boolean).join(' ')} aria-label={ariaLabel}>
      {items.map((item) => (
        <div className="summary-fact" key={item.label}>
          <dt>{item.label}</dt>
          <dd>{item.value}</dd>
        </div>
      ))}
    </dl>
  )
}
