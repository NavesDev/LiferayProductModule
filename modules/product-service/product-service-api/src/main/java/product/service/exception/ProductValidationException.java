package product.service.exception;

import com.liferay.portal.kernel.exception.PortalException;

public class ProductValidationException extends PortalException {

	public ProductValidationException(String msg) {
		super(msg);
	}

}
