/**
 * UserSelect Component
 * Dropdown selector for users with search functionality
 * Filters out already-assigned users and provides search by email/name
 */

import React, { useState, useMemo, useRef, useEffect } from 'react';
import type { IUsuario } from '../types';

/**
 * Props for UserSelect component
 */
export interface UserSelectProps {
  /** List of available users to select from */
  users: IUsuario[];

  /** Currently selected user */
  value: IUsuario | null;

  /** Callback when user selection changes */
  onChange: (user: IUsuario) => void;

  /** List of user IDs to exclude from selection (already assigned) */
  excludeUserIds?: number[];

  /** Label text displayed above the select */
  label?: string;

  /** Mark the field as required (shows asterisk) */
  required?: boolean;

  /** Error message to display below the select */
  error?: string | null;

  /** Disable the select input */
  disabled?: boolean;

  /** Show search input when true or when list > 5 users */
  showSearch?: boolean;

  /** Loading state indicator */
  loading?: boolean;

  /** Additional CSS classes */
  className?: string;

  /** Placeholder text for input */
  placeholder?: string;
}

/**
 * UserSelect Component
 *
 * Provides a dropdown selector for choosing users with:
 * - Search functionality by name or email
 * - Exclusion of already-assigned users
 * - Loading state handling
 * - Error display
 * - Keyboard navigation (Esc to close)
 *
 * Features:
 * - Uncontrolled dropdown state (opens/closes locally)
 * - Filterable user list with search
 * - User avatars with initials
 * - Email display for disambiguation
 * - Accessible ARIA labels
 * - Responsive design
 *
 * @component
 * @example
 * const [selectedUser, setSelectedUser] = useState<IUsuario | null>(null);
 *
 * <UserSelect
 *   users={users}
 *   value={selectedUser}
 *   onChange={setSelectedUser}
 *   excludeUserIds={[1, 2, 3]}
 *   label="Seleccionar Usuario"
 *   required
 * />
 */
export const UserSelect: React.FC<UserSelectProps> = ({
  users,
  value,
  onChange,
  excludeUserIds = [],
  label,
  required = false,
  error,
  disabled = false,
  showSearch: forceShowSearch = false,
  loading = false,
  className = '',
  placeholder = 'Seleccionar usuario...',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const dropdownRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  // Filter out excluded users
  const availableUsers = useMemo(() => {
    return users.filter((u) => !excludeUserIds.includes(u.id));
  }, [users, excludeUserIds]);

  // Filter by search term
  const filteredUsers = useMemo(() => {
    if (!searchTerm) return availableUsers;

    const term = searchTerm.toLowerCase();
    return availableUsers.filter(
      (u) =>
        u.nombre.toLowerCase().includes(term) ||
        u.email.toLowerCase().includes(term)
    );
  }, [availableUsers, searchTerm]);

  // Show search when list is large or forced
  const shouldShowSearch = forceShowSearch || availableUsers.length > 5;

  // Handle user selection
  const handleSelectUser = (user: IUsuario) => {
    onChange(user);
    setIsOpen(false);
    setSearchTerm('');
  };

  // Close dropdown on Escape key
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      setIsOpen(false);
      setSearchTerm('');
    }
  };

  // Focus search input when dropdown opens
  useEffect(() => {
    if (isOpen && shouldShowSearch && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [isOpen, shouldShowSearch]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
        setSearchTerm('');
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () =>
        document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen]);

  // Get user initials
  const getInitials = (name: string): string => {
    return name
      .split(' ')
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('');
  };

  return (
    <div className={`w-full ${className}`}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {label}
          {required && <span className="text-red-500 ml-1">*</span>}
        </label>
      )}

      <div className="relative" ref={dropdownRef}>
        {/* Main button */}
        <button
          type="button"
          onClick={() => !disabled && setIsOpen(!isOpen)}
          onKeyDown={handleKeyDown}
          disabled={disabled || loading}
          className={`w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-left flex items-center justify-between ${
            isOpen ? 'border-blue-500 ring-1 ring-blue-500' : ''
          } ${disabled ? 'bg-gray-100 cursor-not-allowed' : 'bg-white hover:border-gray-400'} ${
            error ? 'border-red-300' : ''
          }`}
          aria-haspopup="listbox"
          aria-expanded={isOpen}
          aria-label={label || placeholder}
        >
          <span className="flex items-center gap-2 truncate">
            {value ? (
              <>
                <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center text-xs font-semibold text-blue-700 shrink-0">
                  {getInitials(value.nombre)}
                </div>
                <div className="truncate">
                  <div className="text-sm font-medium text-gray-900 truncate">
                    {value.nombre}
                  </div>
                  <div className="text-xs text-gray-500 truncate">
                    {value.email}
                  </div>
                </div>
              </>
            ) : (
              <span className="text-gray-500">{placeholder}</span>
            )}
          </span>

          {/* Chevron icon */}
          <svg
            className={`w-4 h-4 shrink-0 transition-transform ${
              isOpen ? 'rotate-180' : ''
            }`}
            viewBox="0 0 20 20"
            fill="currentColor"
            aria-hidden="true"
          >
            <path
              fillRule="evenodd"
              d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
              clipRule="evenodd"
            />
          </svg>
        </button>

        {/* Dropdown menu */}
        {isOpen && (
          <div
            className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-md shadow-lg"
            role="listbox"
          >
            {/* Search input */}
            {shouldShowSearch && (
              <div className="p-2 border-b border-gray-200">
                <input
                  ref={searchInputRef}
                  type="text"
                  placeholder="Buscar usuario..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyDown={handleKeyDown}
                  className="w-full px-2 py-1 border border-gray-300 rounded text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
                  aria-label="Buscar usuarios"
                />
              </div>
            )}

            {/* User list */}
            <ul className="max-h-60 overflow-auto">
              {loading ? (
                <li className="px-3 py-2 text-sm text-gray-500 text-center">
                  Cargando usuarios...
                </li>
              ) : filteredUsers.length === 0 ? (
                <li className="px-3 py-2 text-sm text-gray-500 text-center">
                  {searchTerm
                    ? 'No se encontraron usuarios'
                    : 'No hay usuarios disponibles'}
                </li>
              ) : (
                filteredUsers.map((user) => (
                  <li key={user.id}>
                    <button
                      type="button"
                      onClick={() => handleSelectUser(user)}
                      className="w-full text-left px-3 py-2 hover:bg-blue-50 focus:bg-blue-50 focus:outline-none flex items-center gap-2"
                      role="option"
                      aria-selected={value?.id === user.id}
                    >
                      <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center text-xs font-semibold text-blue-700 shrink-0">
                        {getInitials(user.nombre)}
                      </div>
                      <div className="truncate">
                        <div className="text-sm font-medium text-gray-900">
                          {user.nombre}
                        </div>
                        <div className="text-xs text-gray-500">
                          {user.email}
                        </div>
                      </div>
                      {value?.id === user.id && (
                        <svg
                          className="w-4 h-4 text-blue-600 ml-auto shrink-0"
                          viewBox="0 0 20 20"
                          fill="currentColor"
                          aria-hidden="true"
                        >
                          <path
                            fillRule="evenodd"
                            d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                            clipRule="evenodd"
                          />
                        </svg>
                      )}
                    </button>
                  </li>
                ))
              )}
            </ul>
          </div>
        )}
      </div>

      {/* Error message */}
      {error && (
        <p className="mt-1 text-sm text-red-600" role="alert">
          {error}
        </p>
      )}
    </div>
  );
};

export default UserSelect;
