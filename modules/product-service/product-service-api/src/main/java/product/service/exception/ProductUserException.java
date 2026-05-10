package product.service.exception;

import com.liferay.portal.kernel.exception.PortalException;

public class ProductUserException extends PortalException {

	public ProductUserException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

}
