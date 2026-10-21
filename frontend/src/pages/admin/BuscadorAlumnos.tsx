import { useEffect, useId, useMemo, useRef, useState } from 'react';
import type { CourseItem, StudentItem } from '../../api/admin';
import { coincideBusqueda } from '../../utils/texto';
import { etiquetaCurso } from './alumnoConfirmaciones';

const MAXIMO = 8;

/**
 * Buscador de alumnos del admin: se escribe apellido, nombre o cédula y se abre la ficha.
 * Atajo: Ctrl+K (o ⌘K) o "/" fuera de un campo de texto.
 */
export default function BuscadorAlumnos({ alumnos, cursos, onElegir }: {
  alumnos: StudentItem[];
  cursos: CourseItem[];
  onElegir: (alumno: StudentItem) => void;
}) {
  const [query, setQuery] = useState('');
  const [abierto, setAbierto] = useState(false);
  const [activo, setActivo] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listId = useId();
  const cursoPorId = useMemo(() => new Map(cursos.map((curso) => [curso.id, curso])), [cursos]);

  const resultados = useMemo(() => {
    if (!query.trim()) return [];
    return alumnos.filter((alumno) => coincideBusqueda(query, alumno.apellido, alumno.nombre, alumno.ci))
      .sort((a, b) => a.apellido.localeCompare(b.apellido, 'es') || a.nombre.localeCompare(b.nombre, 'es'))
      .slice(0, MAXIMO);
  }, [alumnos, query]);

  useEffect(() => { setActivo(0); }, [query]);

  useEffect(() => {
    const atajo = (event: KeyboardEvent) => {
      const target = event.target instanceof Element ? event.target : null;
      const escribiendo = target?.closest('input, textarea, select, [contenteditable="true"]');
      if (((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') || (event.key === '/' && !escribiendo)) {
        event.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener('keydown', atajo);
    return () => window.removeEventListener('keydown', atajo);
  }, []);

  const elegir = (alumno: StudentItem) => {
    onElegir(alumno);
    setQuery('');
    setAbierto(false);
    inputRef.current?.blur();
  };

  const onKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'ArrowDown') { event.preventDefault(); setAbierto(true); setActivo((i) => Math.min(i + 1, resultados.length - 1)); }
    else if (event.key === 'ArrowUp') { event.preventDefault(); setActivo((i) => Math.max(i - 1, 0)); }
    else if (event.key === 'Enter' && resultados[activo]) { event.preventDefault(); elegir(resultados[activo]); }
    else if (event.key === 'Escape') { setQuery(''); setAbierto(false); }
  };

  const mostrarLista = abierto && query.trim().length > 0;

  return <div className="buscador-alumnos">
    <label className="visually-hidden" htmlFor={`${listId}-input`}>Buscar alumno</label>
    <svg className="buscador-alumnos-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4.5 4.5" /></svg>
    <input
      ref={inputRef}
      id={`${listId}-input`}
      type="search"
      role="combobox"
      aria-expanded={mostrarLista}
      aria-controls={listId}
      aria-autocomplete="list"
      aria-activedescendant={mostrarLista && resultados[activo] ? `${listId}-${resultados[activo].id}` : undefined}
      placeholder="Buscar alumno…"
      title="Buscar alumno por apellido, nombre o cédula"
      value={query}
      autoComplete="off"
      onChange={(event) => { setQuery(event.target.value); setAbierto(true); }}
      onFocus={() => setAbierto(true)}
      onBlur={() => window.setTimeout(() => setAbierto(false), 120)}
      onKeyDown={onKeyDown}
    />
    <kbd className="buscador-alumnos-kbd" aria-hidden="true">Ctrl K</kbd>
    {mostrarLista && <ul className="buscador-alumnos-lista" id={listId} role="listbox" aria-label="Alumnos encontrados">
      {resultados.length === 0 && <li className="buscador-alumnos-vacio" role="presentation">Ningún alumno coincide.</li>}
      {resultados.map((alumno, index) => {
        const curso = cursoPorId.get(alumno.cursoId);
        return <li key={alumno.id} id={`${listId}-${alumno.id}`} role="option" aria-selected={index === activo}
          className={index === activo ? 'is-active' : undefined}
          onMouseDown={(event) => { event.preventDefault(); elegir(alumno); }}
          onMouseEnter={() => setActivo(index)}>
          <strong>{alumno.apellido}, {alumno.nombre}</strong>
          <span>{curso ? etiquetaCurso(curso) : 'Sin curso'}{alumno.ci ? ` · CI ${alumno.ci}` : ''}</span>
        </li>;
      })}
    </ul>}
  </div>;
}
