package product.service.exception;

import com.liferay.portal.kernel.exception.PortalException;

public class ProductPersistenceException extends PortalException {

	public ProductPersistenceException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

}
