package com.docflow.documentcore.domain.exception;

/**
 * Exception thrown when an ACL (Access Control List) entry for a folder is not found.
 * 
 * This exception is used to indicate that an attempt to revoke or retrieve a specific
 * ACL entry has failed because the entry does not exist in the database.
 */
public class AclNotFoundException extends RuntimeException {
    private static final String MESSAGE_BY_CARPETA_USUARIO = 
        "ACL not found for user %d in folder %d";
    private static final String MESSAGE_BY_ID = 
        "ACL not found with ID %d";
    
    /**
     * Creates an exception indicating that an ACL for a specific user and folder was not found.
     * 
     * @param carpetaId the ID of the folder
     * @param usuarioId the ID of the user
     */
    public AclNotFoundException(Long carpetaId, Long usuarioId) {
        super(String.format(MESSAGE_BY_CARPETA_USUARIO, usuarioId, carpetaId));
    }
    
    /**
     * Creates an exception indicating that an ACL with a specific ID was not found.
     * 
     * @param aclId the ID of the ACL entry
     */
    public AclNotFoundException(Long aclId) {
        super(String.format(MESSAGE_BY_ID, aclId));
    }
}
