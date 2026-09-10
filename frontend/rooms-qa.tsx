import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import SalasPanel from './src/pages/admin/SalasPanel';
import AsignacionesPanel from './src/pages/admin/AsignacionesPanel';
import './src/index.css';
const especialidades = ['Construcciones Civiles','Electrónica','Informática','Electricidad','Mecánica industrial','Electromecánica','Química industrial','Mecánica automotriz'].map((nombre, i) => ({id:i+1,nombre}));
const rooms = Array.from({length:22},(_,i)=>({id:i+1,nombre:i<10?`PC ${String(i+1).padStart(2,'0')}`:`S${i-9}`,especialidadId:i<10?null:1,especialidadNombre:i<10?null:'Construcciones Civiles',bloquesAsignados:i%3===0?8:0}));
window.fetch = async () => new Response(JSON.stringify(rooms),{headers:{'Content-Type':'application/json'}});
const data = {especialidades,usuarios:[{id:1,nombre:'Ana María',apellido:'Acosta',usuario:'aacosta',nivel:1,correo:null},{id:2,nombre:'Juan',apellido:'Benítez',usuario:'jbenitez',nivel:1,correo:null}],cursos:[{id:1,especialidad:'Informática',nivel:1,seccion:'A'},{id:2,especialidad:'Electrónica',nivel:2,seccion:'B'}],asignaciones:Array.from({length:5},(_,i)=>({id:i+1,profesorId:1,materiaId:1,cursoId:i<3?1:2,profesor:'Ana',materia:'Materia de ejemplo',curso:'1 A'})),materias:[],alumnos:[],cursosAlumnos:[]};
function Preview(){const [tab,setTab]=useState('salas');return <main style={{maxWidth:1180,margin:'24px auto',padding:20}}><div className="toolbar"><button className="button secondary" onClick={()=>setTab('salas')}>Salas</button><button className="button secondary" onClick={()=>setTab('asignaciones')}>Asignaciones</button></div>{tab==='salas'?<SalasPanel data={data} status={()=>{}}/>:<AsignacionesPanel data={data} reload={async()=>{}} status={()=>{}}/>}</main>}
createRoot(document.getElementById('root')!).render(<Preview/>);
