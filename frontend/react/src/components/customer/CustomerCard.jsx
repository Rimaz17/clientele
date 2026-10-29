import {
    AlertDialog,
    AlertDialogBody, AlertDialogContent,
    AlertDialogFooter, AlertDialogHeader, AlertDialogOverlay,
    Avatar,
    Badge,
    Box,
    Button,
    Center,
    Flex,
    Heading,
    Image,
    Stack,
    Tag,
    Text,
    useColorModeValue, useDisclosure,
} from '@chakra-ui/react';

import {useRef} from 'react'
import {customerProfilePictureUrl, deleteCustomer} from "../../services/client.js";
import {errorNotification, successNotification} from "../../services/notification.js";
import {getChangeErrorMessage} from "../../services/apiErrors.js";
import UpdateCustomerDrawer from "./UpdateCustomerDrawer.jsx";
import {useAuth} from "../context/AuthContext.jsx";
import {useNavigate} from "react-router-dom";

export default function CardWithImage({id, name, email, age, gender, roles = [], fetchCustomers}) {
    const { isOpen, onOpen, onClose } = useDisclosure()
    const cancelRef = useRef()
    const { isAdmin, isCurrentCustomer, logOut } = useAuth()
    const navigate = useNavigate()
    // The API enforces the same rule; hiding the controls only avoids dead ends.
    const canModify = isAdmin || isCurrentCustomer(email)

    return (
        <Center py={6}>
            <Box
                maxW={'300px'}
                minW={'300px'}
                w={'full'}
                m={2}
                bg={useColorModeValue('white', 'gray.800')}
                boxShadow={'lg'}
                rounded={'md'}
                overflow={'hidden'}>
                <Image
                    h={'120px'}
                    w={'full'}
                    src={
                        'https://images.unsplash.com/photo-1612865547334-09cb8cb455da?ixid=MXwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHw%3D&ixlib=rb-1.2.1&auto=format&fit=crop&w=634&q=80'
                    }
                    objectFit={'cover'}
                />
                <Flex justify={'center'} mt={-12}>
                    <Avatar
                        size={'xl'}
                        src={customerProfilePictureUrl(id)}
                        alt={'Author'}
                        css={{
                            border: '2px solid white',
                        }}
                    />
                </Flex>

                <Box p={6}>
                    <Stack spacing={2} align={'center'} mb={5}>
                        <Stack direction={'row'} spacing={2}>
                            <Tag borderRadius={"full"}>{id}</Tag>
                            {roles.includes("ROLE_ADMIN") && (
                                <Badge colorScheme={'purple'} alignSelf={'center'}>Admin</Badge>
                            )}
                        </Stack>
                        <Heading fontSize={'2xl'} fontWeight={500} fontFamily={'body'}>
                            {name}
                        </Heading>
                        <Text color={'gray.500'}>{email}</Text>
                        <Text color={'gray.500'}>Age {age} | {gender}</Text>
                    </Stack>
                </Box>
                {canModify && (
                    <Stack direction={'row'} justify={'center'} spacing={6} p={4}>
                        <Stack>
                            <UpdateCustomerDrawer
                                initialValues={{ name, email, age }}
                                customerId={id}
                                fetchCustomers={fetchCustomers}
                            />
                        </Stack>
                        <Stack>
                            <Button
                                bg={'red.400'}
                                color={'white'}
                                rounded={'full'}
                                _hover={{
                                    transform: 'translateY(-2px)',
                                    boxShadow: 'lg'
                                }}
                                _focus={{
                                    bg: 'green.500'
                                }}
                                onClick={onOpen}
                            >
                                Delete
                            </Button>
                            <AlertDialog
                                isOpen={isOpen}
                                leastDestructiveRef={cancelRef}
                                onClose={onClose}
                            >
                                <AlertDialogOverlay>
                                    <AlertDialogContent>
                                        <AlertDialogHeader fontSize='lg' fontWeight='bold'>
                                            Delete Customer
                                        </AlertDialogHeader>

                                        <AlertDialogBody>
                                            Are you sure you want to delete {name}? You can&apos;t undo this action afterwards.
                                        </AlertDialogBody>

                                        <AlertDialogFooter>
                                            <Button ref={cancelRef} onClick={onClose}>
                                                Cancel
                                            </Button>
                                            <Button colorScheme='red' onClick={() => {
                                                deleteCustomer(id).then(res => {
                                                    console.log(res)
                                                    successNotification(
                                                        'Customer deleted',
                                                        `${name} was successfully deleted`
                                                    )
                                                    // The token of a deleted account no longer works.
                                                    if (isCurrentCustomer(email)) {
                                                        logOut()
                                                        navigate("/")
                                                        return
                                                    }
                                                    fetchCustomers();

                                                }).catch(err => {
                                                    console.log(err);
                                                    errorNotification(
                                                        "Could not delete customer",
                                                        getChangeErrorMessage(err)
                                                    )
                                                }).finally(() => {
                                                    onClose()
                                                })
                                            }} ml={3}>
                                                Delete
                                            </Button>
                                        </AlertDialogFooter>
                                    </AlertDialogContent>
                                </AlertDialogOverlay>
                            </AlertDialog>
                        </Stack>

                    </Stack>
                )}
            </Box>
        </Center>
    );
}