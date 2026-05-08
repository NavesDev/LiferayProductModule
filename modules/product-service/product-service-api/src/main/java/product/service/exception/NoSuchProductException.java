/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
package product.service.exception;

import com.liferay.portal.kernel.exception.NoSuchModelException;

/**
 * @author Brian Wing Shun Chan
 */
public class NoSuchProductException extends NoSuchModelException {

	public NoSuchProductException() {
	}

	public NoSuchProductException(String msg) {
		super(msg);
	}

	public NoSuchProductException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

	public NoSuchProductException(Throwable throwable) {
		super(throwable);
	}

}