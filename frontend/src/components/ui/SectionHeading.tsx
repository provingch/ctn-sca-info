interface SectionHeadingProps {
  number: string;
  title: string;
  detail: string;
}

export default function SectionHeading({ number, title, detail }: SectionHeadingProps) {
  return <header className="panel-heading">
    <span aria-hidden="true">{number}</span>
    <div>
      <h2>{title}</h2>
      <p>{detail}</p>
    </div>
  </header>;
}
