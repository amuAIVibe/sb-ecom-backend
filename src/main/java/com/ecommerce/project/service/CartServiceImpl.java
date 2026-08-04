package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Service
public class CartServiceImpl implements CartService{
    private final CartRepository cartRepository;
    private final AuthUtil authUtil;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ModelMapper modelMapper;

    public CartServiceImpl(
            CartRepository cartRepository,
            AuthUtil authUtil,
            ProductRepository productRepository,
            CartItemRepository cartItemRepository,
            ModelMapper modelMapper) {
        this.cartRepository = cartRepository;
        this.authUtil = authUtil;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {

        //Find existing cart for a user or create new cart
        Cart cart = createCart();
        //Retrieve Product details
        Product product = productRepository
                .findById(productId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product","productId", productId)
                );
        //Validations like stock exists or not
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(
                cart.getCartId(),
                productId
        );
        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + " already exists");
        }

        if (product.getQuantity() == 0){
            throw new APIException("Product " + product.getProductName() + "is not available");
        }

        if(product.getQuantity() < quantity){
            throw new APIException("Please, make an order of the"
                    + product.getProductName()
                    + " less than or equal to the quantity"
            + product.getQuantity());
        }

        //Create cart item
        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());
        cart.getCartItems().add(newCartItem);
        //Save Cart Item
        cartItemRepository.save(newCartItem);
        product.setQuantity(product.getQuantity());
        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepository.save(cart);
        //Return updated cart
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        //List<CartItem> cartItems = cart.getCartItems();
        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setQuantity(item.getQuantity());
            return map;
        });
        cartDTO.setProducts(productStream.toList());
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();

        if (carts.isEmpty()) {
            throw new APIException("No cart exists");
        }

        List<CartDTO> cartDTOs = carts.stream()
                .map(
                        cart -> {
                            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
                            //CartDTO has the ProductDTO that's why
                            //we need to convert product from Cart to productDTO
                            List<ProductDTO> productDTOS = cart.getCartItems().stream()
                                    .map(item -> {
                                        ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
                                        productDTO.setQuantity(item.getQuantity());
                                        return productDTO;
                                       }
                                    ).collect(Collectors.toList());
                            cartDTO.setProducts(productDTOS);
                            return cartDTO;
                        }
                ).collect(Collectors.toList());
        return cartDTOs;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
        if(cart == null){
            throw new ResourceNotFoundException("Cart","cartId", cartId);
        }
        cart.getCartItems().forEach(
                item -> item.getProduct().setQuantity(item.getQuantity())
        );
        List<ProductDTO> productDTOs = cart.getCartItems().stream()
                .map( item -> modelMapper.map(item.getProduct(), ProductDTO.class)
                ).toList();
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        cartDTO.setProducts(productDTOs);
        return cartDTO;
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String emailId = authUtil.loggedInEmail();
        Long cartId = cartRepository.findCartByEmail(emailId).getCartId();
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId", cartId));
        Product product = productRepository
                .findById(productId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product","productId", productId)
                );

        if (product.getQuantity() == 0){
            throw new APIException("Product " + product.getProductName() + " is not available");
        }

        if(product.getQuantity() < quantity){
            throw new APIException("Please, make an order of the"
                    + product.getProductName()
                    + " less than or equal to the quantity"
                    + product.getQuantity());
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId, cartId);
        if(cartItem == null){
            throw new APIException("Product " + product.getProductName() + " is not available in the cart");
        }

        int newQuantity = cartItem.getQuantity() + quantity;
        if (newQuantity < 0){
            throw new APIException("The product quantity cannot be negative");
        }

        if(newQuantity == 0) {
            deleteProductFromCart(cartId, productId);
        } else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
            cartRepository.save(cart);
        }

        CartItem updatedCartItem = cartItemRepository.save(cartItem);

        //If the quantity of a product reduced to 0 then remove it from the cart
        if (updatedCartItem.getQuantity() == 0) {
            cartItemRepository.deleteById(updatedCartItem.getCartItemId());
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productDTOStream = cartItems.stream().map(
                item -> {
                    ProductDTO prd = modelMapper.map(item.getProduct(), ProductDTO.class);
                    prd.setQuantity(item.getQuantity());
                    return prd;
                });

        cartDTO.setProducts(productDTOStream.toList());
        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId).orElseThrow(
                () -> new ResourceNotFoundException("Cart","cartId", cartId)
        );

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId, cartId);
        if(cartItem == null){
            throw new ResourceNotFoundException("Product","productId", productId);
        }
        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));
        cartItemRepository.deleteCartItemByProductIdAndCartId(productId, cartId);
        return "Product "+cartItem.getProduct().getProductName()+" has been removed from the cart!!";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId", cartId));
        Product product = productRepository
                .findById(productId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product","productId", productId)
                );
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if(cartItem == null){
            throw new APIException("Product " + product.getProductName() + " is not available in the cart");
        }

        double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());
        cart.setTotalPrice(
                cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity())
        );

        cartItem = cartItemRepository.save(cartItem);
    }

    private @NonNull Stream<ProductDTO> getProductDTOStream(Cart cart) {
        List<CartItem> cartItems = cart.getCartItems();

        //This portion is done to set ProductDTO to CartDTO
        //As per CartDTO model
        return cartItems.stream()
                .map(item ->
                    {
                        ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
                        //This setQuantity is done to set the quantity from cart item
                        //otherwise it will set quantity from the ProductDTO
                        map.setQuantity(item.getQuantity());
                        return map;
                    }
                );
    }

    private Cart createCart(){
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if(userCart != null){
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);
    }
}
