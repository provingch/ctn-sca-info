import { useCallback, useEffect, useId, useRef, useState, type CSSProperties, type KeyboardEvent } from 'react';
import './AnimatedSelect.css';
import { createPortal } from 'react-dom';

export interface AnimatedSelectOption {
  value: string | number;
  label: string;
  disabled?: boolean;
}

interface AnimatedSelectProps {
  value: string | number | string[];
  multiple?: boolean;
  portal?: boolean;
  options: AnimatedSelectOption[];
  onChange: (value: string) => void;
  placeholder?: string;
  ariaLabel: string;
  describedBy?: string;
  name?: string;
  required?: boolean;
  disabled?: boolean;
  className?: string;
}

export default function AnimatedSelect({
  value,
  options,
  onChange,
  placeholder = 'Seleccione…',
  ariaLabel,
  describedBy,
  name,
  required = false,
  disabled = false,
  className = '',
  multiple = false,
  portal = false,
}: AnimatedSelectProps) {
  const generatedId = useId().replace(/:/g, '');
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);
  const [position, setPosition] = useState<CSSProperties>({});
  const [open, setOpen] = useState(false);
  const [openUpward, setOpenUpward] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [topGradientOpacity, setTopGradientOpacity] = useState(0);
  const [bottomGradientOpacity, setBottomGradientOpacity] = useState(0);
  const stringValue = String(value ?? '');
  const selectedIndex = options.findIndex((option) => String(option.value) === stringValue);
  const selected = selectedIndex >= 0 ? options[selectedIndex] : null;
  const values = Array.isArray(value) ? value : [stringValue];
  const selectedLabel = multiple ? options.filter((option) => values.includes(String(option.value))).map((option) => option.label).join(', ') : selected?.label;
  const listboxId = `animated-select-list-${generatedId}`;

  const updateGradients = useCallback(() => {
    const list = listRef.current;
    if (!list) return;
    const bottomDistance = list.scrollHeight - list.scrollTop - list.clientHeight;
    setTopGradientOpacity(Math.min(list.scrollTop / 36, 1));
    setBottomGradientOpacity(list.scrollHeight <= list.clientHeight ? 0 : Math.min(bottomDistance / 36, 1));
  }, []);

  const firstEnabled = useCallback(() => options.findIndex((option) => !option.disabled), [options]);

  const openList = useCallback((preferredIndex = selectedIndex) => {
    if (disabled) return;
    const nextIndex = preferredIndex >= 0 && !options[preferredIndex]?.disabled ? preferredIndex : firstEnabled();
    const bounds = rootRef.current?.getBoundingClientRect();
    if (bounds) {
      const spaceBelow = window.innerHeight - bounds.bottom;
      setOpenUpward(spaceBelow < 300 && bounds.top > spaceBelow);
    }
    setActiveIndex(nextIndex);
    setOpen(true);
  }, [disabled, firstEnabled, options, selectedIndex]);

  useEffect(() => {
    if (!open) return;
    const closeOutside = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node) && !popoverRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('pointerdown', closeOutside);
    return () => document.removeEventListener('pointerdown', closeOutside);
  }, [open]);

  useEffect(() => {
    if (!open || !portal) return;
    const place = () => {
      const rect = triggerRef.current?.getBoundingClientRect();
      if (!rect) return;
      const below = window.innerHeight - rect.bottom - 12;
      const upward = below < 260 && rect.top > below;
      const width = Math.min(Math.max(rect.width, 220), window.innerWidth - 24);
      setPosition({ position: 'fixed', width,
        left: Math.max(12, Math.min(rect.left, window.innerWidth - width - 12)),
        top: upward ? 'auto' : rect.bottom + 6, bottom: upward ? window.innerHeight - rect.top + 6 : 'auto',
        '--select-list-height': `${Math.max(80, Math.min(300, (upward ? rect.top - 12 : below) - 20))}px`,
      } as CSSProperties);
    };
    place();
    const onScroll = (event: Event) => { if (!popoverRef.current?.contains(event.target as Node)) place(); };
    window.addEventListener('resize', place); document.addEventListener('scroll', onScroll, true);
    return () => { window.removeEventListener('resize', place); document.removeEventListener('scroll', onScroll, true); };
  }, [open, portal]);

  useEffect(() => {
    if (!open) return;
    const frame = requestAnimationFrame(() => {
      updateGradients();
      const list = listRef.current;
      const option = list?.querySelector<HTMLElement>(`[data-option-index="${activeIndex}"]`);
      if (list && option) {
        if (option.offsetTop < list.scrollTop) list.scrollTop = option.offsetTop;
        else if (option.offsetTop + option.offsetHeight > list.scrollTop + list.clientHeight) list.scrollTop = option.offsetTop + option.offsetHeight - list.clientHeight;
      }
    });
    return () => cancelAnimationFrame(frame);
  }, [activeIndex, open, updateGradients]);

  function moveActive(direction: 1 | -1) {
    if (options.length === 0) return;
    let index = activeIndex;
    for (let attempts = 0; attempts < options.length; attempts += 1) {
      index = (index + direction + options.length) % options.length;
      if (!options[index].disabled) {
        setActiveIndex(index);
        return;
      }
    }
  }

  function choose(index: number) {
    const option = options[index];
    if (!option || option.disabled) return;
    onChange(String(option.value));
    setActiveIndex(index);
    if (!multiple) setOpen(false);
    triggerRef.current?.focus();
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (disabled) return;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!open) openList(event.key === 'ArrowDown' ? selectedIndex : selectedIndex >= 0 ? selectedIndex : firstEnabled());
      else moveActive(event.key === 'ArrowDown' ? 1 : -1);
    } else if (event.key === 'Home' && open) {
      event.preventDefault();
      setActiveIndex(firstEnabled());
    } else if (event.key === 'End' && open) {
      event.preventDefault();
      const lastEnabled = options.findLastIndex((option) => !option.disabled);
      setActiveIndex(lastEnabled);
    } else if ((event.key === 'Enter' || event.key === ' ') && open) {
      event.preventDefault();
      choose(activeIndex);
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      openList();
    } else if (event.key === 'Escape' && open) {
      event.preventDefault();
      setOpen(false);
    } else if (event.key === 'Tab') {
      setOpen(false);
    }
  }

  const popover = <div ref={popoverRef} className={`animated-select-popover ${portal ? "animated-select-portal" : ""}`} style={portal ? position : undefined} data-specialty={portal ? rootRef.current?.closest("[data-specialty]")?.getAttribute("data-specialty") ?? undefined : undefined}>
      <div
        ref={listRef}
        id={listboxId}
        className="animated-select-list"
        role="listbox"
        aria-multiselectable={multiple || undefined}
        aria-label={ariaLabel}
        onScroll={updateGradients}
      >
        {options.length === 0 && <div className="animated-select-empty">No hay opciones disponibles</div>}
        {options.map((option, index) => <button
          id={`${listboxId}-option-${index}`}
          data-option-index={index}
          type="button"
          role="option"
          aria-selected={(multiple ? values.includes(String(option.value)) : index === selectedIndex)}
          disabled={option.disabled}
          key={String(option.value)}
          className={`animated-select-option ${index === activeIndex ? 'active' : ''} ${(multiple ? values.includes(String(option.value)) : index === selectedIndex) ? 'selected' : ''}`}
          style={{ animationDelay: `${Math.min(index * 22, 176)}ms` } as CSSProperties}
          onPointerEnter={() => setActiveIndex(index)}
          onClick={() => choose(index)}
        >
          <span>{option.label}</span><i aria-hidden="true">✓</i>
        </button>)}
      </div>
      <div className="animated-select-gradient top" style={{ opacity: topGradientOpacity }} />
      <div className="animated-select-gradient bottom" style={{ opacity: bottomGradientOpacity }} />
    </div>;

  return <div ref={rootRef} className={`animated-select ${open ? 'open' : ''} ${openUpward ? 'open-upward' : ''} ${disabled ? 'disabled' : ''} ${className}`}>
    <select
      className="animated-select-native"
      tabIndex={-1}
      aria-hidden="true"
      name={name}
      required={required}
      disabled={disabled}
      value={multiple ? values : stringValue}
      multiple={multiple}
      onChange={() => undefined}
      onInvalid={(event) => {
        event.preventDefault();
        openList();
        triggerRef.current?.focus();
      }}
    >
      <option value="">{placeholder}</option>
      {options.map((option) => <option key={String(option.value)} value={String(option.value)} disabled={option.disabled}>{option.label}</option>)}
    </select>
    <button
      ref={triggerRef}
      type="button"
      className="animated-select-trigger"
      aria-label={ariaLabel}
      aria-describedby={describedBy}
      aria-haspopup="listbox"
      aria-expanded={open}
      aria-controls={listboxId}
      aria-activedescendant={open && activeIndex >= 0 ? `${listboxId}-option-${activeIndex}` : undefined}
      aria-required={required}
      disabled={disabled}
      onClick={() => open ? setOpen(false) : openList()}
      onKeyDown={handleKeyDown}
    >
      <span className={selectedLabel ? '' : 'placeholder'}>{selectedLabel || placeholder}</span>
      <svg viewBox="0 0 20 20" aria-hidden="true"><path d="m5 7.5 5 5 5-5" /></svg>
    </button>
    {open && (portal ? createPortal(popover, document.body) : popover)}
  </div>;
}
