import React, { useState } from 'react';
import { FormField, Input, Button, Select } from '@ui/forms';
import { FORM_LABELS, VALIDATION_MESSAGES } from '../../constants/messages';
import type { CreateUserFormData, UserRole } from '../../types/user.types';

interface CreateUserFormProps {
    /** Callback al enviar el formulario */
    onSubmit: (data: CreateUserFormData) => void;
    /** Indica si está procesando */
    isLoading: boolean;
    /** Callback para cancelar */
    onCancel: () => void;
    /** Roles disponibles para asignar */
    availableRoles: UserRole[];
}

/** Expresión regular para validar formato de email */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Longitud mínima de contraseña */
const MIN_PASSWORD_LENGTH = 8;

/**
 * Formulario de creación de usuario con validaciones frontend
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const CreateUserForm: React.FC<CreateUserFormProps> = ({
    onSubmit,
    isLoading,
    onCancel,
    availableRoles = [],
}) => {
    // Estado del formulario
    const [email, setEmail] = useState('');
    const [nombre, setNombre] = useState('');
    const [password, setPassword] = useState('');
    const [rolId, setRolId] = useState<number | ''>('');
    const [errors, setErrors] = useState<Record<string, string>>({});

    // Validaciones del formulario
    const validate = (): boolean => {
        const newErrors: Record<string, string> = {};

        // Validar email
        if (!email.trim()) {
            newErrors.email = VALIDATION_MESSAGES.EMAIL_REQUIRED;
        } else if (!EMAIL_REGEX.test(email)) {
            newErrors.email = VALIDATION_MESSAGES.EMAIL_INVALID;
        }

        // Validar nombre
        if (!nombre.trim()) {
            newErrors.nombre = VALIDATION_MESSAGES.NAME_REQUIRED;
        } else if (nombre.trim().length < 2) {
            newErrors.nombre = 'El nombre debe tener al menos 2 caracteres';
        }

        // Validar contraseña
        if (!password) {
            newErrors.password = VALIDATION_MESSAGES.PASSWORD_REQUIRED;
        } else if (password.length < MIN_PASSWORD_LENGTH) {
            newErrors.password = VALIDATION_MESSAGES.PASSWORD_MIN_LENGTH;
        }

        // Validar rol
        if (!rolId) {
            newErrors.rolId = 'Debe seleccionar un rol';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    // Manejar envío del formulario
    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (validate()) {
            onSubmit({
                email: email.trim(),
                nombre: nombre.trim(),
                password,
                rolId: Number(rolId)
            });
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <FormField
                label={FORM_LABELS.EMAIL}
                error={errors.email}
                required
                htmlFor="create-user-email"
            >
                <Input
                    id="create-user-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="usuario@ejemplo.com"
                    disabled={isLoading}
                    autoComplete="email"
                />
            </FormField>

            <FormField
                label={FORM_LABELS.NAME}
                error={errors.nombre}
                required
                htmlFor="create-user-name"
            >
                <Input
                    id="create-user-name"
                    type="text"
                    value={nombre}
                    onChange={(e) => setNombre(e.target.value)}
                    placeholder="Nombre completo"
                    disabled={isLoading}
                    autoComplete="name"
                />
            </FormField>

            <FormField
                label="Rol Inicial"
                error={errors.rolId}
                required
                htmlFor="create-user-role"
            >
                <Select
                    id="create-user-role"
                    value={rolId}
                    onChange={(e) => setRolId(Number(e.target.value))}
                    disabled={isLoading || availableRoles.length === 0}
                >
                    {availableRoles.length === 0 ? (
                        <option value="" disabled>No hay roles disponibles</option>
                    ) : (
                        <>
                            <option value="">Seleccione un rol...</option>
                            {availableRoles.map((role) => (
                                <option key={role.id} value={role.id}>
                                    {role.nombre}
                                </option>
                            ))}
                        </>
                    )}
                </Select>
            </FormField>

            <FormField
                label={FORM_LABELS.PASSWORD}
                error={errors.password}
                required
                htmlFor="create-user-password"
            >
                <Input
                    id="create-user-password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Mínimo 8 caracteres"
                    disabled={isLoading}
                    autoComplete="new-password"
                />
            </FormField>

            <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
                <Button
                    type="button"
                    variant="secondary"
                    onClick={onCancel}
                    disabled={isLoading}
                    fullWidth={false}
                >
                    {FORM_LABELS.CANCEL}
                </Button>
                <Button
                    type="submit"
                    loading={isLoading}
                    fullWidth={false}
                >
                    {FORM_LABELS.CREATE_USER}
                </Button>
            </div>
        </form>
    );
};
