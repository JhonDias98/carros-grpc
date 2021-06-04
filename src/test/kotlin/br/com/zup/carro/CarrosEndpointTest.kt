package br.com.zup.carro

import br.com.zup.CarroRequest
import br.com.zup.CarrosGrpcServiceGrpc
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

//Rodar testes por linha de comando: .\gradlew.bat test
@MicronautTest(transactional = false)
internal class CarrosEndpointTest(
    val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub,
    val carrosRepository: CarroRepository
) {

    /*
    Cenários:
    1 - Tudo certo
    2 - Quando já existe carro com a placa
    3 - Quando os dados de entrada são invalidos
     */

    @BeforeEach
    internal fun setUp() {
        carrosRepository.deleteAll()
    }


    @Test
    fun `Deve adicionar um novo carro`(){

        //Cenário - BeforeEach

        //Ação
        val response = grpcClient.adicionar(CarroRequest.newBuilder()
            .setModelo("Gol")
            .setPlaca("HPX-1234")
            .build())

        //Validação
        with(response){
            assertNotNull(id)
            assertTrue(carrosRepository.existsById(id)) //Efeito colateral
        }

    }

    @Test
    fun `Nao deve adicionar novo carro quando placa ja for existente`() {

        //Cenário - BeforeEach
        val existente = carrosRepository.save(Carro("Golzin", "OTZ-0004"))

        //Ação
        val error = assertThrows<StatusRuntimeException> { //Para pegar a excepion
            grpcClient.adicionar(
                CarroRequest.newBuilder()
                    .setModelo("Gol")
                    .setPlaca(existente.placa)
                    .build()
            )
        }

        //Validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code)
        }

    }

    @Test
    fun `Nao deve adicionar novo carro quando dados de entrada forem invalidos`() {

        //Cenário - BeforeEach

        //Ação
        val error = assertThrows<StatusRuntimeException> { //Para pegar a excepion
            grpcClient.adicionar(
                CarroRequest.newBuilder()
                    .setModelo("Palio")
                    .setPlaca("")
                    .build()
            )
        }

        //Validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertEquals("dados de entrada inválidos", this.status.description)
        }
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

}