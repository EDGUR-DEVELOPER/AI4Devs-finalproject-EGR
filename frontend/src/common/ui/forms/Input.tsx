import React from 'react';

export interface InputProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'className'> {
  error?: string;
  fullWidth?: boolean;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ error, fullWidth = true, disabled, ...props }, ref) => {
    const baseClasses = 'input-field';
    const widthClasses = fullWidth ? 'w-full' : '';
    const errorClasses = error ? 'border-red-500 focus:ring-red-500' : '';
    const disabledClasses = disabled ? 'opacity-50 cursor-not-allowed' : '';

    return (
      <input
        ref={ref}
        disabled={disabled}
        aria-invalid={!!error}
        aria-describedby={error ? `${props.id}-error` : undefined}
        className={`${baseClasses} ${widthClasses} ${errorClasses} ${disabledClasses}`.trim()}
        {...props}
      />
    );
  }
);

Input.displayName = 'Input';
