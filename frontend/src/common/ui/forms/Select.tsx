import React from 'react';

export interface SelectProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'className'> {
    error?: string;
    fullWidth?: boolean;
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
    ({ error, fullWidth = true, disabled, children, ...props }, ref) => {
        const baseClasses = 'input-field';
        const widthClasses = fullWidth ? 'w-full' : '';
        const errorClasses = error ? 'border-red-500 focus:ring-red-500' : '';
        const disabledClasses = disabled ? 'opacity-50 cursor-not-allowed' : '';

        return (
            <select
                ref={ref}
                disabled={disabled}
                aria-invalid={!!error}
                aria-describedby={error ? `${props.id}-error` : undefined}
                className={`${baseClasses} ${widthClasses} ${errorClasses} ${disabledClasses}`.trim()}
                {...props}
            >
                {children}
            </select>
        );
    }
);

Select.displayName = 'Select';
