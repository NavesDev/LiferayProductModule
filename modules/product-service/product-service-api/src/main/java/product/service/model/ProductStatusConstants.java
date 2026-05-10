package product.service.model;

public class ProductStatusConstants {

	public static final int DRAFT = 0;

	public static final int PUBLISHED = 1;

	public static final int INACTIVE = 2;

	public static boolean isValid(int status) {
		return (status == DRAFT) || (status == PUBLISHED) ||
			(status == INACTIVE);
	}

	private ProductStatusConstants() {
	}

}
