package product.service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetEntry;
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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import product.service.exception.ProductCategoryException;
import product.service.exception.ProductStatusException;
import product.service.exception.ProductTagException;
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
	private AssetEntry assetEntry;

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

	@InjectMocks
	private ProductLocalServiceImpl productLocalService;

	@BeforeEach
	void setUp() {
		assertThat(productLocalService).isNotNull();
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
				eq(USER_ID), eq(GROUP_ID), any(Date.class), any(Date.class),
				eq(Product.class.getName()), eq(PRODUCT_ID), anyString(), eq(0L),
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
	@DisplayName("Buscar produtos por grupo")
	class GetProductsByGroupId {

		@Test
		@DisplayName("Dado grupo com produtos, quando buscar, entao retorna lista do grupo")
		void dado_grupoComProdutos_quando_buscar_entao_retornaListaDoGrupo() {

			// Arrange
			Product p1 = _product(ProductStatusConstants.DRAFT);
			Product p2 = _product(ProductStatusConstants.PUBLISHED);

			when(productPersistence.findByGroupId(GROUP_ID)).thenReturn(
				List.of(p1, p2));

			// Act
			List<Product> result = productLocalService.getProductsByGroupId(GROUP_ID);

			// Assert
			assertThat(result).hasSize(2).containsExactly(p1, p2);

			verify(productPersistence).findByGroupId(GROUP_ID);
		}

		@Test
		@DisplayName("Dado grupo sem produtos, quando buscar, entao retorna lista vazia")
		void dado_grupoSemProdutos_quando_buscar_entao_retornaListaVazia() {

			// Arrange
			when(productPersistence.findByGroupId(GROUP_ID)).thenReturn(List.of());

			// Act
			List<Product> result = productLocalService.getProductsByGroupId(GROUP_ID);

			// Assert
			assertThat(result).isEmpty();

			verify(productPersistence).findByGroupId(GROUP_ID);
		}

	}

	@Nested
	@DisplayName("Atualizar categorias do produto")
	class UpdateProductCategories {

		@Test
		@DisplayName("Dado produto draft com categoria valida, quando atualizar, entao sincroniza asset")
		void dado_produtoDraftComCategoriaValida_quando_atualizar_entao_sincronizaAsset()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(
				assetCategory);
			when(assetCategory.getGroupId()).thenReturn(GROUP_ID);
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);
			when(assetEntry.getTagNames()).thenReturn(new String[] {"tag"});

			// Act
			Product result = productLocalService.updateProductCategories(
				PRODUCT_ID, new long[] {CATEGORY_ID}, _serviceContext());

			// Assert
			assertThat(result).isSameAs(product);

			verify(assetEntryLocalService).updateEntry(
				eq(USER_ID), eq(GROUP_ID), any(Date.class), any(Date.class),
				eq(Product.class.getName()), eq(PRODUCT_ID), anyString(), eq(0L),
				aryEq(new long[] {CATEGORY_ID}), any(String[].class),
				anyBoolean(), anyBoolean(), nullable(Date.class),
				nullable(Date.class), nullable(Date.class), nullable(Date.class),
				nullable(String.class), anyString(), anyString(),
				nullable(String.class), nullable(String.class),
				nullable(String.class), anyInt(), anyInt(), anyDouble(),
				any(ServiceContext.class));
		}

		@Test
		@DisplayName("Dado produto published sem categorias, quando atualizar, entao rejeita")
		void dado_produtoPublishedSemCategorias_quando_atualizar_entao_rejeita()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.PUBLISHED);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.updateProductCategories(
					PRODUCT_ID, new long[0], _serviceContext())
			).isInstanceOf(ProductValidationException.class);
		}

	}

	@Nested
	@DisplayName("Atualizar tags do produto")
	class UpdateProductTags {

		@Test
		@DisplayName("Dado produto com tags validas, quando atualizar, entao sincroniza asset com novas tags")
		void dado_produtoComTagsValidas_quando_atualizar_entao_sincronizaAssetComNovasTags()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(assetTagLocalService.fetchAssetTag(TAG_ID)).thenReturn(assetTag);
			when(assetTag.getGroupId()).thenReturn(GROUP_ID);
			when(assetTag.getName()).thenReturn("nova-tag");
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[] {CATEGORY_ID});

			// Act
			Product result = productLocalService.updateProductTags(
				PRODUCT_ID, new long[] {TAG_ID}, _serviceContext());

			// Assert
			assertThat(result).isSameAs(product);

			verify(assetEntryLocalService).updateEntry(
				eq(USER_ID), eq(GROUP_ID), any(Date.class), any(Date.class),
				eq(Product.class.getName()), eq(PRODUCT_ID), anyString(), eq(0L),
				any(long[].class), aryEq(new String[] {"nova-tag"}),
				anyBoolean(), anyBoolean(), nullable(Date.class),
				nullable(Date.class), nullable(Date.class), nullable(Date.class),
				nullable(String.class), anyString(), anyString(),
				nullable(String.class), nullable(String.class),
				nullable(String.class), anyInt(), anyInt(), anyDouble(),
				any(ServiceContext.class));
		}

		@Test
		@DisplayName("Dado tag de outro grupo, quando atualizar, entao rejeita")
		void dado_tagDeOutroGrupo_quando_atualizar_entao_rejeita()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(assetTagLocalService.fetchAssetTag(TAG_ID)).thenReturn(assetTag);
			when(assetTag.getGroupId()).thenReturn(99999L);
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.updateProductTags(
					PRODUCT_ID, new long[] {TAG_ID}, _serviceContext())
			).isInstanceOf(ProductTagException.class);
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

		@Test
		@DisplayName("Dado draft valido com categoria, quando publicar, entao atualiza status e sincroniza asset")
		void dado_draftValidoComCategoria_quando_publicar_entao_atualizaStatusESincronizaAsset()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[] {CATEGORY_ID});
			when(assetEntry.getTagNames()).thenReturn(new String[] {"tag-1"});

			// Act
			Product updated = productLocalService.updateProductStatus(
				PRODUCT_ID, ProductStatusConstants.PUBLISHED, _serviceContext());

			// Assert
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.PUBLISHED);

			verify(productPersistence).update(productArgumentCaptor.capture());

			assertThat(productArgumentCaptor.getValue().getStatus()).isEqualTo(
				ProductStatusConstants.PUBLISHED);
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

}
