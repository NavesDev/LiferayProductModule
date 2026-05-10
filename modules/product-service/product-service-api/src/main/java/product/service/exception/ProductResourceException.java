package product.service.exception;

import com.liferay.portal.kernel.exception.PortalException;

public class ProductResourceException extends PortalException {

	public ProductResourceException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

}
