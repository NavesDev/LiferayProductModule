package product.service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ResourceLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;

import java.util.Date;
import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import product.service.exception.ProductCategoryException;
import product.service.exception.ProductStatusException;
import product.service.exception.ProductValidationException;
import product.service.model.Product;
import product.service.model.ProductStatusConstants;
import product.service.model.impl.ProductImpl;
import product.service.service.persistence.ProductPersistence;

@DisplayName("ProductLocalServiceImpl")
@ExtendWith(MockitoExtension.class)
class ProductLocalServiceImplTest {

	private static final long CATEGORY_ID = 301L;
	private static final long COMPANY_ID = 10001L;
	private static final String DESCRIPTION = "Descricao do produto";
	private static final long GROUP_ID = 30001L;
	private static final String NAME = "Produto teste";
	private static final double PRICE = 99.90D;
	private static final long PRODUCT_ID = 40001L;
	private static final int STOCK_QUANTITY = 10;
	private static final long TAG_ID = 501L;
	private static final long USER_ID = 20001L;
	private static final String USER_NAME = "Test User";

	@Mock
	private AssetCategory assetCategory;

	@Mock
	private AssetCategoryLocalService assetCategoryLocalService;

	@Mock
	private AssetEntryLocalService assetEntryLocalService;

	@Mock
	private AssetTag assetTag;

	@Mock
	private AssetTagLocalService assetTagLocalService;

	@Mock
	private CounterLocalService counterLocalService;

	@Mock
	private ProductPersistence productPersistence;

	@Mock
	private ResourceLocalService resourceLocalService;

	@Mock
	private User user;

	@Mock
	private UserLocalService userLocalService;

	@Captor
	private ArgumentCaptor<Product> productArgumentCaptor;

	private ProductLocalServiceImpl productLocalService;

	@BeforeEach
	void setUp() throws Exception {
		productLocalService = new ProductLocalServiceImpl();

		_setField(
			productLocalService, "assetCategoryLocalService",
			assetCategoryLocalService);
		_setField(
			productLocalService, "assetEntryLocalService", assetEntryLocalService);
		_setField(productLocalService, "assetTagLocalService", assetTagLocalService);
		_setField(productLocalService, "counterLocalService", counterLocalService);
		_setField(productLocalService, "productPersistence", productPersistence);
		_setField(
			productLocalService, "resourceLocalService", resourceLocalService);
		_setField(productLocalService, "userLocalService", userLocalService);
	}

	@Nested
	@DisplayName("Adicionar produto")
	class AddProduct {

		@Test
		@DisplayName("Dado draft valido, quando adicionar, entao persiste produto e sincroniza asset")
		void dado_draftValido_quando_adicionar_entao_persisteProdutoESincronizaAsset()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();
			Product createdProduct = new ProductImpl();

			createdProduct.setProductId(PRODUCT_ID);

			when(counterLocalService.increment(Product.class.getName())).thenReturn(
				PRODUCT_ID);
			when(productPersistence.create(PRODUCT_ID)).thenReturn(createdProduct);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));
			when(userLocalService.getUser(USER_ID)).thenReturn(user);
			when(user.getCompanyId()).thenReturn(COMPANY_ID);
			when(user.getFullName()).thenReturn(USER_NAME);
			when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(
				assetCategory);
			when(assetCategory.getGroupId()).thenReturn(GROUP_ID);
			when(assetTagLocalService.fetchAssetTag(TAG_ID)).thenReturn(assetTag);
			when(assetTag.getGroupId()).thenReturn(GROUP_ID);
			when(assetTag.getName()).thenReturn("tag-1");

			// Act
			Product product = productLocalService.addProduct(
				USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
				ProductStatusConstants.DRAFT, STOCK_QUANTITY,
				new long[] {CATEGORY_ID}, new long[] {TAG_ID}, serviceContext);

			// Assert
			assertThat(product.getProductId()).isEqualTo(PRODUCT_ID);
			assertThat(product.getStatus()).isEqualTo(ProductStatusConstants.DRAFT);
			assertThat(product.getStockQuantity()).isEqualTo(STOCK_QUANTITY);

			verify(productPersistence).update(productArgumentCaptor.capture());

			Product persistedProduct = productArgumentCaptor.getValue();

			assertThat(persistedProduct.getName()).isEqualTo(NAME);
			assertThat(persistedProduct.getDescription()).isEqualTo(DESCRIPTION);
			assertThat(persistedProduct.getPrice()).isEqualTo(PRICE);
			assertThat(persistedProduct.getUserName()).isEqualTo(USER_NAME);

			verify(assetEntryLocalService).updateEntry(
				anyLong(), anyLong(), any(Date.class), any(Date.class),
				anyString(), anyLong(), anyString(), anyLong(),
				aryEq(new long[] {CATEGORY_ID}), aryEq(new String[] {"tag-1"}),
				anyBoolean(), anyBoolean(), nullable(Date.class),
				nullable(Date.class), nullable(Date.class), nullable(Date.class),
				nullable(String.class), anyString(), anyString(),
				nullable(String.class), nullable(String.class),
				nullable(String.class), anyInt(), anyInt(), anyDouble(),
				any(ServiceContext.class));
		}

		@Test
		@DisplayName("Dado published sem categorias, quando adicionar, entao bloqueia publicacao")
		void dado_publishedSemCategorias_quando_adicionar_entao_bloqueiaPublicacao()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.PUBLISHED, STOCK_QUANTITY, new long[0],
					new long[0], serviceContext)
			).isInstanceOf(ProductValidationException.class);
		}

		@Test
		@DisplayName("Dado estoque negativo, quando adicionar, entao rejeita")
		void dado_estoqueNegativo_quando_adicionar_entao_rejeita()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.DRAFT, -1, new long[0], new long[0],
					serviceContext)
			).isInstanceOf(ProductValidationException.class);
		}

		@Test
		@DisplayName("Dado categoria de outro grupo, quando adicionar, entao rejeita")
		void dado_categoriaDeOutroGrupo_quando_adicionar_entao_rejeita()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(
				assetCategory);
			when(assetCategory.getGroupId()).thenReturn(99999L);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.DRAFT, STOCK_QUANTITY,
					new long[] {CATEGORY_ID}, new long[0], serviceContext)
			).isInstanceOf(ProductCategoryException.class);
		}
	}

	@Nested
	@DisplayName("Atualizar status")
	class UpdateProductStatus {

		@Test
		@DisplayName("Dado published para draft, quando atualizar status, entao rejeita transicao")
		void dado_publishedParaDraft_quando_atualizarStatus_entao_rejeitaTransicao()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.PUBLISHED);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.updateProductStatus(
					PRODUCT_ID, ProductStatusConstants.DRAFT, _serviceContext())
			).isInstanceOf(ProductStatusException.class);
		}
	}

	@Nested
	@DisplayName("Remover produto")
	class DeleteProduct {

		@Test
		@DisplayName("Dado produto existente, quando remover, entao remove asset e persiste exclusao")
		void dado_produtoExistente_quando_remover_entao_removeAssetEPersisteExclusao()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(productPersistence.remove(any(Product.class))).thenReturn(product);

			// Act
			Product deletedProduct = productLocalService.deleteProduct(PRODUCT_ID);

			// Assert
			assertThat(deletedProduct).isSameAs(product);
			verify(assetEntryLocalService).deleteEntry(
				Product.class.getName(), PRODUCT_ID);
			verify(productPersistence).remove(product);
		}
	}

	private Product _product(int status) {
		Product product = new ProductImpl();

		product.setCompanyId(COMPANY_ID);
		product.setCreateDate(new Date());
		product.setDescription(DESCRIPTION);
		product.setGroupId(GROUP_ID);
		product.setModifiedDate(new Date());
		product.setName(NAME);
		product.setPrice(PRICE);
		product.setProductId(PRODUCT_ID);
		product.setStatus(status);
		product.setStockQuantity(STOCK_QUANTITY);
		product.setUserId(USER_ID);
		product.setUserName(USER_NAME);
		product.setUuid("product-uuid");

		return product;
	}

	private ServiceContext _serviceContext() {
		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(COMPANY_ID);
		serviceContext.setScopeGroupId(GROUP_ID);
		serviceContext.setUserId(USER_ID);

		return serviceContext;
	}

	private void _setField(Object target, String fieldName, Object value)
		throws Exception {

		Class<?> clazz = target.getClass();

		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(fieldName);

				field.setAccessible(true);
				field.set(target, value);

				return;
			}
			catch (NoSuchFieldException noSuchFieldException) {
				clazz = clazz.getSuperclass();
			}
		}

		throw new NoSuchFieldException(fieldName);
	}

}
