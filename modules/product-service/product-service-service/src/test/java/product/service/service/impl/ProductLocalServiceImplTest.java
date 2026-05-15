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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.portal.kernel.exception.PortalException;
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

import product.service.exception.ProductAssetUpdateException;
import product.service.exception.ProductCategoryException;
import product.service.exception.ProductPersistenceException;
import product.service.exception.ProductStatusException;
import product.service.exception.ProductTagException;
import product.service.exception.ProductUserException;
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
		@DisplayName("Dado serviceContext nulo, quando adicionar, entao cria novo serviceContext e persiste")
		void dado_serviceContextNulo_quando_adicionar_entao_criaNovoeServiceContextEPersiste()
			throws Exception {

			// Arrange
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

			// Act
			Product product = productLocalService.addProduct(
				USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
				ProductStatusConstants.DRAFT, STOCK_QUANTITY, new long[0],
				new long[0], null);

			// Assert
			assertThat(product.getStatus()).isEqualTo(ProductStatusConstants.DRAFT);
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
		@DisplayName("Dado status invalido, quando adicionar, entao rejeita")
		void dado_statusInvalido_quando_adicionar_entao_rejeita()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE, 999,
					STOCK_QUANTITY, new long[0], new long[0], serviceContext)
			).isInstanceOf(ProductStatusException.class);
		}

		@Test
		@DisplayName("Dado userId invalido, quando adicionar, entao lanca ProductUserException")
		void dado_userIdInvalido_quando_adicionar_entao_lancaProductUserException()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			when(userLocalService.getUser(USER_ID)).thenThrow(
				new PortalException("User not found"));

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.DRAFT, STOCK_QUANTITY, new long[0],
					new long[0], serviceContext)
			).isInstanceOf(ProductUserException.class);
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

		@Test
		@DisplayName("Dado categoria nao encontrada, quando adicionar, entao lanca ProductCategoryException")
		void dado_categoriaNaoEncontrada_quando_adicionar_entao_lancaProductCategoryException()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(
				null);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.DRAFT, STOCK_QUANTITY,
					new long[] {CATEGORY_ID}, new long[0], serviceContext)
			).isInstanceOf(ProductCategoryException.class);
		}

		@Test
		@DisplayName("Dado tag nao encontrada, quando adicionar, entao lanca ProductTagException")
		void dado_tagNaoEncontrada_quando_adicionar_entao_lancaProductTagException()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			when(assetTagLocalService.fetchAssetTag(TAG_ID)).thenReturn(null);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.DRAFT, STOCK_QUANTITY, new long[0],
					new long[] {TAG_ID}, serviceContext)
			).isInstanceOf(ProductTagException.class);
		}

		@Test
		@DisplayName("Dado tag de outro grupo, quando adicionar, entao lanca ProductTagException")
		void dado_tagDeOutroGrupo_quando_adicionar_entao_lancaProductTagException()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			when(assetTagLocalService.fetchAssetTag(TAG_ID)).thenReturn(assetTag);
			when(assetTag.getGroupId()).thenReturn(99999L);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.DRAFT, STOCK_QUANTITY, new long[0],
					new long[] {TAG_ID}, serviceContext)
			).isInstanceOf(ProductTagException.class);
		}

		@Test
		@DisplayName("Dado falha na persistencia, quando adicionar, entao lanca ProductPersistenceException")
		void dado_falhaNaPersistencia_quando_adicionar_entao_lancaProductPersistenceException()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();
			Product createdProduct = new ProductImpl();

			createdProduct.setProductId(PRODUCT_ID);

			when(counterLocalService.increment(Product.class.getName())).thenReturn(
				PRODUCT_ID);
			when(productPersistence.create(PRODUCT_ID)).thenReturn(createdProduct);
			when(productPersistence.update(any(Product.class))).thenThrow(
				new RuntimeException("DB error"));
			when(userLocalService.getUser(USER_ID)).thenReturn(user);
			when(user.getCompanyId()).thenReturn(COMPANY_ID);
			when(user.getFullName()).thenReturn(USER_NAME);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.DRAFT, STOCK_QUANTITY, new long[0],
					new long[0], serviceContext)
			).isInstanceOf(ProductPersistenceException.class);
		}

		@Test
		@DisplayName("Dado falha no asset update, quando adicionar, entao lanca ProductAssetUpdateException")
		void dado_falhaNoAssetUpdate_quando_adicionar_entao_lancaProductAssetUpdateException()
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
			doThrow(new PortalException("asset error")).when(
				assetEntryLocalService).updateEntry(
					anyLong(), anyLong(), any(), any(), anyString(), anyLong(),
					anyString(), anyLong(), any(long[].class), any(String[].class),
					anyBoolean(), anyBoolean(), nullable(Date.class),
					nullable(Date.class), nullable(Date.class), nullable(Date.class),
					nullable(String.class), anyString(), anyString(),
					nullable(String.class), nullable(String.class),
					nullable(String.class), anyInt(), anyInt(), anyDouble(),
					any(ServiceContext.class));

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.DRAFT, STOCK_QUANTITY, new long[0],
					new long[0], serviceContext)
			).isInstanceOf(ProductAssetUpdateException.class);
		}

		@Test
		@DisplayName("Dado published com todos os campos validos e categorias, quando adicionar, entao persiste publicado")
		void dado_publishedComCamposValidosECategorias_quando_adicionar_entao_persistePublicado()
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

			// Act
			Product product = productLocalService.addProduct(
				USER_ID, GROUP_ID, NAME, DESCRIPTION, PRICE,
				ProductStatusConstants.PUBLISHED, STOCK_QUANTITY,
				new long[] {CATEGORY_ID}, new long[0], serviceContext);

			// Assert
			assertThat(product.getStatus()).isEqualTo(ProductStatusConstants.PUBLISHED);
		}

		@Test
		@DisplayName("Dado published com nome em branco, quando adicionar, entao rejeita")
		void dado_publishedComNomeEmBranco_quando_adicionar_entao_rejeita()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, "", DESCRIPTION, PRICE,
					ProductStatusConstants.PUBLISHED, STOCK_QUANTITY,
					new long[] {CATEGORY_ID}, new long[0], serviceContext)
			).isInstanceOf(ProductValidationException.class);
		}

		@Test
		@DisplayName("Dado published com descricao em branco, quando adicionar, entao rejeita")
		void dado_publishedComDescricaoEmBranco_quando_adicionar_entao_rejeita()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, "", PRICE,
					ProductStatusConstants.PUBLISHED, STOCK_QUANTITY,
					new long[] {CATEGORY_ID}, new long[0], serviceContext)
			).isInstanceOf(ProductValidationException.class);
		}

		@Test
		@DisplayName("Dado published com preco negativo, quando adicionar, entao rejeita")
		void dado_publishedComPrecoNegativo_quando_adicionar_entao_rejeita()
			throws Exception {

			// Arrange
			ServiceContext serviceContext = _serviceContext();

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.addProduct(
					USER_ID, GROUP_ID, NAME, DESCRIPTION, -1.0,
					ProductStatusConstants.PUBLISHED, STOCK_QUANTITY,
					new long[] {CATEGORY_ID}, new long[0], serviceContext)
			).isInstanceOf(ProductValidationException.class);
		}

	}

	@Nested
	@DisplayName("Atualizar produto")
	class UpdateProduct {

		@Test
		@DisplayName("Dado draft valido, quando atualizar, entao persiste alteracoes e sincroniza asset")
		void dado_draftValido_quando_atualizar_entao_persisteAlteracoesESincronizaAsset()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));
			when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(
				assetCategory);
			when(assetCategory.getGroupId()).thenReturn(GROUP_ID);
			when(assetTagLocalService.fetchAssetTag(TAG_ID)).thenReturn(assetTag);
			when(assetTag.getGroupId()).thenReturn(GROUP_ID);
			when(assetTag.getName()).thenReturn("tag-1");

			// Act
			Product updated = productLocalService.updateProduct(
				PRODUCT_ID, "Novo nome", "Nova descricao", 199.90,
				ProductStatusConstants.DRAFT, 5, new long[] {CATEGORY_ID},
				new long[] {TAG_ID}, _serviceContext());

			// Assert
			assertThat(updated.getName()).isEqualTo("Novo nome");
			assertThat(updated.getDescription()).isEqualTo("Nova descricao");
			assertThat(updated.getPrice()).isEqualTo(199.90);
			assertThat(updated.getStockQuantity()).isEqualTo(5);
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.DRAFT);

			verify(productPersistence).update(productArgumentCaptor.capture());

			assertThat(productArgumentCaptor.getValue().getName()).isEqualTo(
				"Novo nome");
		}

		@Test
		@DisplayName("Dado draft para published com categorias, quando atualizar, entao publica produto")
		void dado_draftParaPublished_quando_atualizar_entao_publicaProduto()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));
			when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(
				assetCategory);
			when(assetCategory.getGroupId()).thenReturn(GROUP_ID);

			// Act
			Product updated = productLocalService.updateProduct(
				PRODUCT_ID, NAME, DESCRIPTION, PRICE,
				ProductStatusConstants.PUBLISHED, STOCK_QUANTITY,
				new long[] {CATEGORY_ID}, new long[0], _serviceContext());

			// Assert
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.PUBLISHED);
		}

		@Test
		@DisplayName("Dado serviceContext nulo, quando atualizar, entao cria novo serviceContext")
		void dado_serviceContextNulo_quando_atualizar_entao_criaNovoeServiceContext()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));

			// Act
			Product updated = productLocalService.updateProduct(
				PRODUCT_ID, NAME, DESCRIPTION, PRICE, ProductStatusConstants.DRAFT,
				STOCK_QUANTITY, new long[0], new long[0], null);

			// Assert
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.DRAFT);
		}

		@Test
		@DisplayName("Dado transicao invalida published para draft, quando atualizar, entao lanca ProductStatusException")
		void dado_transicaoInvalidaPublishedParaDraft_quando_atualizar_entao_lancaProductStatusException()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.PUBLISHED);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.updateProduct(
					PRODUCT_ID, NAME, DESCRIPTION, PRICE,
					ProductStatusConstants.DRAFT, STOCK_QUANTITY, new long[0],
					new long[0], _serviceContext())
			).isInstanceOf(ProductStatusException.class);
		}

		@Test
		@DisplayName("Dado estoque negativo, quando atualizar, entao lanca ProductValidationException")
		void dado_estoqueNegativo_quando_atualizar_entao_lancaProductValidationException()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.updateProduct(
					PRODUCT_ID, NAME, DESCRIPTION, PRICE, ProductStatusConstants.DRAFT,
					-1, new long[0], new long[0], _serviceContext())
			).isInstanceOf(ProductValidationException.class);
		}

		@Test
		@DisplayName("Dado inactive para draft, quando atualizar, entao aceita transicao")
		void dado_inactiveParaDraft_quando_atualizar_entao_aceitaTransicao()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.INACTIVE);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));

			// Act
			Product updated = productLocalService.updateProduct(
				PRODUCT_ID, NAME, DESCRIPTION, PRICE, ProductStatusConstants.DRAFT,
				STOCK_QUANTITY, new long[0], new long[0], _serviceContext());

			// Assert
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.DRAFT);
		}

		@Test
		@DisplayName("Dado inactive para published com categorias, quando atualizar, entao aceita transicao")
		void dado_inactiveParaPublished_quando_atualizar_entao_aceitaTransicao()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.INACTIVE);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));
			when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(
				assetCategory);
			when(assetCategory.getGroupId()).thenReturn(GROUP_ID);

			// Act
			Product updated = productLocalService.updateProduct(
				PRODUCT_ID, NAME, DESCRIPTION, PRICE, ProductStatusConstants.PUBLISHED,
				STOCK_QUANTITY, new long[] {CATEGORY_ID}, new long[0],
				_serviceContext());

			// Assert
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.PUBLISHED);
		}

		@Test
		@DisplayName("Dado draft para inactive, quando atualizar, entao aceita transicao")
		void dado_draftParaInactive_quando_atualizar_entao_aceitaTransicao()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));

			// Act
			Product updated = productLocalService.updateProduct(
				PRODUCT_ID, NAME, DESCRIPTION, PRICE, ProductStatusConstants.INACTIVE,
				STOCK_QUANTITY, new long[0], new long[0], _serviceContext());

			// Assert
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.INACTIVE);
		}

		@Test
		@DisplayName("Dado mesmo status, quando atualizar, entao aceita sem validar transicao")
		void dado_mesmoStatus_quando_atualizar_entao_aceitaSemValidarTransicao()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));

			// Act
			Product updated = productLocalService.updateProduct(
				PRODUCT_ID, NAME, DESCRIPTION, PRICE, ProductStatusConstants.DRAFT,
				STOCK_QUANTITY, new long[0], new long[0], _serviceContext());

			// Assert
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.DRAFT);
		}

		@Test
		@DisplayName("Dado published para inactive, quando atualizar, entao aceita transicao")
		void dado_publishedParaInactive_quando_atualizar_entao_aceitaTransicao()
			throws Exception {

			// Arrange
			Product existing = _product(ProductStatusConstants.PUBLISHED);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(existing);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));

			// Act
			Product updated = productLocalService.updateProduct(
				PRODUCT_ID, NAME, DESCRIPTION, PRICE, ProductStatusConstants.INACTIVE,
				STOCK_QUANTITY, new long[0], new long[0], _serviceContext());

			// Assert
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.INACTIVE);
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

		@Test
		@DisplayName("Dado produto draft com assetEntry lancando excecao, quando atualizar, entao retorna array vazio de tags")
		void dado_produtoDraftComAssetEntryFalhando_quando_atualizar_entao_retornaArrayVazioTags()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(assetCategoryLocalService.fetchAssetCategory(CATEGORY_ID)).thenReturn(
				assetCategory);
			when(assetCategory.getGroupId()).thenReturn(GROUP_ID);
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenThrow(
					new PortalException("Asset not found"));

			// Act
			Product result = productLocalService.updateProductCategories(
				PRODUCT_ID, new long[] {CATEGORY_ID}, _serviceContext());

			// Assert
			assertThat(result).isSameAs(product);
		}

		@Test
		@DisplayName("Dado categorias nulas, quando atualizar, entao aceita com array vazio")
		void dado_categoriasNulas_quando_atualizar_entao_aceitaComArrayVazio()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);
			when(assetEntry.getTagNames()).thenReturn(new String[0]);

			// Act
			Product result = productLocalService.updateProductCategories(
				PRODUCT_ID, null, _serviceContext());

			// Assert
			assertThat(result).isSameAs(product);
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

		@Test
		@DisplayName("Dado assetEntry lancando excecao ao buscar categories, quando atualizar tags, entao usa array vazio de categories")
		void dado_assetEntryFalhando_quando_atualizarTags_entao_usaArrayVazioCategories()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenThrow(
					new PortalException("Asset not found"));

			// Act
			Product result = productLocalService.updateProductTags(
				PRODUCT_ID, new long[0], _serviceContext());

			// Assert
			assertThat(result).isSameAs(product);
		}

		@Test
		@DisplayName("Dado tagIds nulos, quando atualizar, entao resolve como array vazio")
		void dado_tagIdsNulos_quando_atualizar_entao_resolveComoArrayVazio()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);

			// Act
			Product result = productLocalService.updateProductTags(
				PRODUCT_ID, null, _serviceContext());

			// Assert
			assertThat(result).isSameAs(product);
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

		@Test
		@DisplayName("Dado published para inactive, quando atualizar status, entao aceita transicao")
		void dado_publishedParaInactive_quando_atualizarStatus_entao_aceitaTransicao()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.PUBLISHED);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(productPersistence.update(any(Product.class))).thenAnswer(
				invocation -> invocation.getArgument(0));
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[] {CATEGORY_ID});
			when(assetEntry.getTagNames()).thenReturn(new String[] {"tag-1"});

			// Act
			Product updated = productLocalService.updateProductStatus(
				PRODUCT_ID, ProductStatusConstants.INACTIVE, _serviceContext());

			// Assert
			assertThat(updated.getStatus()).isEqualTo(ProductStatusConstants.INACTIVE);
		}

		@Test
		@DisplayName("Dado draft para published sem categorias, quando atualizar status, entao rejeita publicacao")
		void dado_draftParaPublishedSemCategorias_quando_atualizarStatus_entao_rejeitaPublicacao()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.findByPrimaryKey(PRODUCT_ID)).thenReturn(product);
			when(assetEntryLocalService.getEntry(
				Product.class.getName(), PRODUCT_ID)).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);

			// Act / Assert
			assertThatThrownBy(
				() -> productLocalService.updateProductStatus(
					PRODUCT_ID, ProductStatusConstants.PUBLISHED, _serviceContext())
			).isInstanceOf(ProductValidationException.class);
		}

	}

	@Nested
	@DisplayName("Remover produto")
	class DeleteProduct {

		@Test
		@DisplayName("Dado productId existente, quando remover por id, entao delega para remover por objeto")
		void dado_productIdExistente_quando_removerPorId_entao_delegaParaRemoverPorObjeto()
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

		@Test
		@DisplayName("Dado objeto produto, quando remover, entao remove asset e persiste exclusao")
		void dado_objetoProduto_quando_remover_entao_removeAssetEPersisteExclusao()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			when(productPersistence.remove(any(Product.class))).thenReturn(product);

			// Act
			Product deletedProduct = productLocalService.deleteProduct(product);

			// Assert
			assertThat(deletedProduct).isSameAs(product);

			verify(assetEntryLocalService).deleteEntry(
				Product.class.getName(), PRODUCT_ID);
			verify(productPersistence).remove(product);
		}

		@Test
		@DisplayName("Dado falha ao deletar asset entry, quando remover, entao continua e remove produto")
		void dado_falhaAoDeletarAssetEntry_quando_remover_entao_continuaERemoveProduto()
			throws Exception {

			// Arrange
			Product product = _product(ProductStatusConstants.DRAFT);

			doThrow(new PortalException("asset delete error")).when(
				assetEntryLocalService).deleteEntry(
					Product.class.getName(), PRODUCT_ID);
			when(productPersistence.remove(any(Product.class))).thenReturn(product);

			// Act
			Product deletedProduct = productLocalService.deleteProduct(product);

			// Assert
			assertThat(deletedProduct).isSameAs(product);
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
