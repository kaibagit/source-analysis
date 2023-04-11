
class CodeSource{

	private URL location;	//代码来源

    /*
     * The code signers.签名者
     */
    private transient CodeSigner[] signers = null;

    /*
     * The code signers. Certificate chains are concatenated.签名证书
     */
    private transient java.security.cert.Certificate certs[] = null;
}


class ProtectionDomain{

	/* CodeSource */
    private CodeSource codesource ;

    /* the rights this protection domain is granted */
    private PermissionCollection permissions;

	public ProtectionDomain(CodeSource codesource,
                            PermissionCollection permissions) {
        。。。
    }
}