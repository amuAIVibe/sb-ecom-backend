package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    ModelMapper modelMapper = new ModelMapper();

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);

        List<Address> addressList = user.getAddresses();
        addressList.add(address);
        user.setAddresses(addressList);

        address.setUser(user);
        Address savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses() {
        List<Address> addressList = addressRepository.findAll();
        return addressList.stream().map(
                address ->
                        modelMapper.map(address, AddressDTO.class)
        ).toList();
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Address","addressId", addressId)
                );
        return modelMapper.map(address, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addressList = user.getAddresses();
        return addressList.stream().map(
                address ->
                        modelMapper.map(address, AddressDTO.class)
        ).toList();
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address addressFromDB = addressRepository.findById(addressId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Address","addressId", addressId)
                );

        addressFromDB.setStreet(addressDTO.getStreet());
        addressFromDB.setBuildingName(addressDTO.getBuildingName());
        addressFromDB.setCity(addressDTO.getCity());
        addressFromDB.setState(addressDTO.getState());
        addressFromDB.setPincode(addressDTO.getPincode());
        addressFromDB.setCountry(addressDTO.getCountry());

        Address updatedAddress = addressRepository.save(addressFromDB);

        //Since User is also having Address as an entity
        //so we need to update the user's address
        User user = addressFromDB.getUser();
        user.getAddresses().removeIf(
                address -> address.getAddressId().equals(addressFromDB.getAddressId())
        );
        user.getAddresses().add(updatedAddress);
        userRepository.save(user);

        return modelMapper.map(updatedAddress, AddressDTO.class);
    }

    @Override
    public String deleteAddress(Long addressId) {
        Address addressFromDB = addressRepository.findById(addressId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Address","addressId", addressId)
                );

        //Since User is also having Address as an entity
        //so we need to delete the address from the User
        User user = addressFromDB.getUser();
        user.getAddresses().removeIf(
                address -> address.getAddressId().equals(addressFromDB.getAddressId())
        );
        userRepository.save(user);

        addressRepository.delete(addressFromDB);
        return "Address has been deleted!!";
    }


}
