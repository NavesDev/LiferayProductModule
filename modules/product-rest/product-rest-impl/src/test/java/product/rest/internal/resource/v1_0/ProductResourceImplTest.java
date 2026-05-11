package product.rest.internal.resource.v1_0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.vulcan.pagination.Page;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import product.rest.dto.v1_0.Product;
import product.rest.dto.v1_0.ProductCategories;
import product.rest.dto.v1_0.ProductTags;
import product.service.model.impl.ProductImpl;
import product.service.service.ProductLocalService;

@DisplayName("ProductResourceImpl")
@ExtendWith(MockitoExtension.class)
class ProductResourceImplTest {

	private static final long COMPANY_ID = 10001L;
	private static final long GROUP_ID = 30001L;
	private static final long PRODUCT_ID = 40001L;
	private static final long SITE_ID = 30001L;
	private static final long USER_ID = 20001L;

	@Mock
	private AssetEntry assetEntry;

	@Mock
	private AssetEntryLocalService assetEntryLocalService;

	@Mock
	private AssetTag assetTag;

	@Mock
	private AssetTagLocalService assetTagLocalService;

	@Mock
	private Company company;

	@Mock
	private ProductLocalService productLocalService;

	@Mock
	private User user;

	@Captor
	private ArgumentCaptor<ServiceContext> serviceContextArgumentCaptor;

	private ProductResourceImpl productResource;

	@BeforeEach
	void setUp() throws Exception {
		productResource = new ProductResourceImpl();

		_setField(productResource, "_assetEntryLocalService", assetEntryLocalService);
		_setField(productResource, "_assetTagLocalService", assetTagLocalService);
		_setField(productResource, "_productLocalService", productLocalService);

		productResource.setContextCompany(company);
		productResource.setContextUser(user);

		lenient().when(company.getCompanyId()).thenReturn(COMPANY_ID);
		lenient().when(user.getUserId()).thenReturn(USER_ID);
		lenient().when(user.getGroupId()).thenReturn(GROUP_ID);
	}

	@Nested
	@DisplayName("Criar produto")
	class PostProduct {

		@Test
		@DisplayName("Dado payload valido, quando criar, entao delega ao local service e retorna DTO")
		void dado_payloadValido_quando_criar_entao_delegaAoLocalServiceERetornaDto()
			throws Exception {

			// Arrange
			Product payload = new Product();
			payload.setName("Produto A");
			payload.setDescription("Descricao A");
			payload.setPrice(19.9D);
			payload.setStatus(Product.Status.DRAFT);
			payload.setStockQuantity(7);
			payload.setCategoryIds(new Long[] {11L});
			payload.setTagIds(new Long[] {22L});

			ProductImpl created = _product(PRODUCT_ID, "Produto A", 7);

			when(
				productLocalService.addProduct(
					anyLong(), anyLong(), anyString(), anyString(), anyDouble(),
					anyInt(), anyInt(), any(long[].class), any(long[].class),
					any(ServiceContext.class))
			).thenReturn(created);
			when(
				assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), PRODUCT_ID)
			).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[] {11L});
			when(assetEntry.getTagNames()).thenReturn(new String[] {"tag-22"});
			when(assetTagLocalService.fetchTag(GROUP_ID, "tag-22")).thenReturn(assetTag);
			when(assetTag.getTagId()).thenReturn(22L);

			// Act
			Product response = productResource.postSiteProduct(SITE_ID, payload);

			// Assert
			assertThat(response.getId()).isEqualTo(PRODUCT_ID);
			assertThat(response.getName()).isEqualTo("Produto A");
			assertThat(response.getCategoryIds()).containsExactly(11L);
			assertThat(response.getTagIds()).containsExactly(22L);

			verify(productLocalService).addProduct(
				anyLong(), anyLong(), anyString(), anyString(), anyDouble(),
				anyInt(), anyInt(), any(long[].class), any(long[].class),
				serviceContextArgumentCaptor.capture());

			assertThat(serviceContextArgumentCaptor.getValue().getScopeGroupId()).isEqualTo(SITE_ID);
			assertThat(serviceContextArgumentCaptor.getValue().getUserId()).isEqualTo(USER_ID);
		}
	}

	@Nested
	@DisplayName("Consultar produtos")
	class GetProducts {

		@Test
		@DisplayName("Dado filtro status, quando listar, entao retorna apenas produtos publicados")
		void dado_filtroStatus_quando_listar_entao_retornaApenasProdutosPublicados()
			throws Exception {

			// Arrange
			ProductImpl publishedInStock = _product(1L, "Notebook", 5);
			publishedInStock.setStatus(1);

			ProductImpl draftOutStock = _product(2L, "Mouse", 0);
			draftOutStock.setStatus(0);

				when(productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
					List.of(publishedInStock, draftOutStock));
			when(
				assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 1L)
			).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntry.getTagNames()).thenReturn(new String[0]);
			when(
				assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 2L)
			).thenReturn(assetEntry);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, "published", null, null, null, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(1);
			assertThat(page.getItems().iterator().next().getId()).isEqualTo(1L);
		}
	}

	@Nested
	@DisplayName("Atualizar classificacao")
	class UpdateClassification {

		@Test
		@DisplayName("Dado categorias, quando atualizar categorias, entao delega e retorna produto")
		void dado_categorias_quando_atualizarCategorias_entao_delegaERetornaProduto()
			throws Exception {

			// Arrange
			ProductCategories productCategories = new ProductCategories();
			productCategories.setCategoryIds(new Long[] {9L, 10L});

			ProductImpl updated = _product(PRODUCT_ID, "Produto A", 3);

			when(
				productLocalService.updateProductCategories(
					anyLong(), any(), any(ServiceContext.class))
			).thenReturn(updated);
			when(productLocalService.getProduct(PRODUCT_ID)).thenReturn(updated);

			// Act
			Product response = productResource.putSiteProductCategories(
				SITE_ID, PRODUCT_ID, productCategories);

			// Assert
			assertThat(response.getId()).isEqualTo(PRODUCT_ID);
			verify(productLocalService).updateProductCategories(
				anyLong(), any(), any(ServiceContext.class));
		}

		@Test
		@DisplayName("Dado tags, quando atualizar tags, entao delega e retorna produto")
		void dado_tags_quando_atualizarTags_entao_delegaERetornaProduto()
			throws Exception {

			// Arrange
			ProductTags productTags = new ProductTags();
			productTags.setTagIds(new Long[] {7L});

			ProductImpl updated = _product(PRODUCT_ID, "Produto A", 3);

			when(
				productLocalService.updateProductTags(
					anyLong(), any(), any(ServiceContext.class))
			).thenReturn(updated);
			when(productLocalService.getProduct(PRODUCT_ID)).thenReturn(updated);

			// Act
			Product response = productResource.putSiteProductTags(
				SITE_ID, PRODUCT_ID, productTags);

			// Assert
			assertThat(response.getId()).isEqualTo(PRODUCT_ID);
			verify(productLocalService).updateProductTags(
				anyLong(), any(), any(ServiceContext.class));
		}
	}

	private ProductImpl _product(long id, String name, int stockQuantity) {
		ProductImpl product = new ProductImpl();

		product.setProductId(id);
		product.setDescription("Descricao");
		product.setGroupId(SITE_ID);
		product.setName(name);
		product.setPrice(10.5D);
		product.setStatus(0);
		product.setStockQuantity(stockQuantity);

		return product;
	}

	private void _setField(Object target, String fieldName, Object value)
		throws Exception {

		Field field = null;
		Class<?> clazz = target.getClass();

		while ((field == null) && (clazz != null)) {
			try {
				field = clazz.getDeclaredField(fieldName);
			}
			catch (NoSuchFieldException noSuchFieldException) {
				clazz = clazz.getSuperclass();
			}
		}

		field.setAccessible(true);
		field.set(target, value);
	}

}
