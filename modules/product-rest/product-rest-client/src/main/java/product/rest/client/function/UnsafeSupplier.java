package product.rest.client.function;

import javax.annotation.Generated;

/**
 * @author naves
 * @generated
 */
@FunctionalInterface
@Generated("")
public interface UnsafeSupplier<T, E extends Throwable> {

	public T get() throws E;

}