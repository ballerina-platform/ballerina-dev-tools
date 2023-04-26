import ballerina/io;
import ballerina/http;

@display {
    label: "Reservation EP",
    id: "MainEP"
}
public function main() returns error? {
    @display {
        label: "",
        id: "002"
    }
    http:Client seat_allocation_client = check new (seatAllocationAPIUrl);

    @display {
        label: "",
        id: "001"
    }
    http:Client reservation_client = check new("http://localhost:9090/reservations/my");
    Reservation reservationData = {
        flightNumber: "ALK128",
        origin: "Chennai",
        destination: "Bandaranaike",
        flightDate: "Wed 04:13PM IST",
        seats: 150
    };
    ConfirmedReservation confirmedRes = check reservation_client->/reservation.post(reservationData);
    io:println(string`Confirmation id is ${confirmedRes.id}`);
}
