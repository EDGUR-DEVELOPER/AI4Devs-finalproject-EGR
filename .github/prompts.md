## commit
prepare a descriptive commit message for the changes

## update-docs
Use `ai-specs/specs/documentation-standards.mdc` to update whatever documentation is needed according to the changes made

## meta-prompt
### Instructions
You are an expert in prompt engineering.

Given the following prompt, prepare it using best-practice structure (role, objective, etc.) and formatting to achieve a precise and comprehensive result. Stick strictly to the requested objective by carefully analyzing what is asked in the original prompt. Give me result in spanish

### Original Prompt:
{Text}

## enrich-us
Please analyze and fix the ticket:
[Texto US]

Follow these steps:

1. You will act as a product expert with technical knowledge

2. Understand the problem described in the ticket

3. Decide whether or not the User Story is completely detailed according to product's best practices: Include a full description of the functionality, a comprehensive list of fields to be updated, the structure and URLs of the necessary endpoints, the files to be modified according to the architecture and best practices, the steps required for the task to be considered complete, how to update any relevant documentation or create unit tests, and non-functional requirements related to security, performance, etc

4. If the user story lacks the technical and specific detail necessary to allow the developer to be fully autonomous when completing it, provide an improved story that is clearer, more specific, and more concise in line with product best practices described in step 3. Use the technical context you will find in @documentation. Return it in markdown format.

5. Update ticket, adding the new content after the old one and marking each section with the h2 tags [original] and [enhanced]. Apply proper formatting to make it readable and visually clear, using appropriate text types (lists, code snippets...).

6. If the ticket status was "To refine", move the task to the "Pending refinement validation" column.

7. Give me result in spanish.

