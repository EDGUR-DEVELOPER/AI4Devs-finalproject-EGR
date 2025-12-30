## Tickets
`Modificacion de Prompt para generar la lista de tickets por epicas`
```
A partir de la siguientes Historias de Usuarios, genera por separado listas completas de tickets necesarios para implementar la funcionalidad desde inicio a fin.

Lista de Historias de Usuario en la Epica:
[Indicar epica con historias de usuario aquí]

Tu tarea:
– Analizar las historias.
– Identificar sus capacidades principales.
– Dividir la funcionalidad en tickets accionables y entregables independientes.
– NO inventar funcionalidades nuevas; solo descomponer la historia.

Estructura obligatoria de salida:

1. Resumen de alcance detectado

– Lista de capacidades encontradas
– Restricciones implícitas
– Riesgos o ambigüedades (si existen)

2. Lista de tickets necesarios (granular, ordenados)

Cada ticket debe estar estructurado así:

Título: [Acción breve]
Objetivo: [Qué resuelve]
Tipo: [Historia / tarea / subtarea / bug / diseño / QA]
Descripción corta: [Máx. 3–4 líneas]
Entregables:
– [Entregable 1]
– [Entregable 2]

(Generar tantos como sean necesarios para completar la historia.)

3. Flujo recomendado de ejecución

– Orden ideal de implementación
– Dependencias entre tickets

4. Recomendación TDD/BDD

– Qué tickets deberían tener pruebas primero
– Qué tickets se prestan a escenarios BDD

5. Generacion de archivos md

- Genera lista de archivos md con el nombre de la US
- Este archivo contendra la lista de tickets por cada US
- Utiliza como ejemplo el contenido de la carpeta [Indicacion de la carpeta]

Reglas del prompt:
– No agregar funcionalidades nuevas que no existan en la historia.
– Si la historia es muy grande, divídela en varias historias y luego en tickets.
– Usa lenguaje claro para producto, diseño, desarrollo y QA.
– Asegura que cada ticket sea independiente, estimable y verificable.
– Prioriza granularidad útil: ni demasiado grande ni demasiado pequeña.
- Dividir los tickets en Base de datos, Backend y Frontend.
- Coloca los archivos en la carpeta [Inidicacion del nombre de la carpeta].
```