import ballerina/http;
import ballerina/log;

configurable string hospitalServicesBackend = "http://localhost:9090";
configurable string paymentBackend = "http://localhost:9090/healthcare/payments";

type Patient record {|
    string name;
    string dob;
    string ssn;
    string address;
    string phone;
    string email;
|};

type ReservationRequest record {|
    record {|
        *Patient;
        string cardNo;
    |} patient;
    string doctor;
    string hospital_id;
    string hospital;
    string appointment_date;
|};

type AppointmentRequest record {|
    Patient patient;
    string doctor;
    string hospital;
    string appointment_date;
|};

type Doctor record {|
    readonly string name;
    string hospital;
    string category;
    string availability;
    decimal fee;
|};

type Appointment record {|
    int appointmentNumber;
    Doctor doctor;
    Patient patient;
    string hospital;
    boolean confirmed;
    string appointmentDate;
|};

type ChannelingFee record {|
    string patientName;
    string doctorName;
    string actualFee;
|};

type PaymentSettlement record {|
    int appointmentNumber;
    Doctor doctor;
    Patient patient;
    decimal fee;
    boolean confirmed;
    string card_number;
|};

type PaymentResponse record {
    int appointmentNo;
    string doctorName;
    string patient;
    int actualFee;
    int discount;
    decimal discounted;
    string paymentID;
    string status;
};

final http:Client hospitalServicesEP = check initializeHttpClient(hospitalServicesBackend);
final http:Client paymentEP = check initializeHttpClient(paymentBackend);

function initializeHttpClient(string url) returns http:Client|error => new (url);

service /healthcare on new http:Listener(9095) {

    resource function post categories/[string doctorCategory]/reserve(ReservationRequest payload) returns PaymentResponse|error {
        final var requestPayload = payload.cloneReadOnly();
        
        // @foo:LogMediator
        worker LogHospitalDetails {
            log:printInfo("ReservationRequest123", payload = requestPayload);
        }

        // @foo:DataMapper
        worker CreateAppointmentPayload {
            AppointmentRequest appointmentReq = AppointmentPayloadMapper(requestPayload);
            appointmentReq -> CreateAppointment;
        }

        // @foo:HttpPostMediator
        worker CreateAppointment returns error? {
            // [JBUG] Multiple Receive actions are not yet supported
            // AppointmentRequest AppointmentReq = <- {function, CreateAppointmentPayload};

            AppointmentRequest appointmentReq = <- CreateAppointmentPayload;
            Appointment appt = check hospitalServicesEP->/[requestPayload.hospital_id]/categories/[doctorCategory]/reserve.post(appointmentReq);

            appt -> LogAppointment;
            appt -> GetAppointmentFee;
        }

        // @foo:LogMediator
        worker LogAppointment returns error? {
            Appointment appointment = check <- CreateAppointment;
            log:printInfo("Appointment", payload = appointment);
        }

        // @foo:HttpGetMediator
        worker GetAppointmentFee returns error? {
            string hospitalId = requestPayload.hospital_id;

            Appointment appointment = check <- CreateAppointment;
            int apptNumber = appointment.appointmentNumber;

            ChannelingFee fee = check hospitalServicesEP->/[hospitalId]/categories/appointments/[apptNumber]/fee;

            fee -> LogAppointmentFee;
            [appointment, fee] -> CreatePaymentRequest;
        }

        // @foo:LogMediator
        worker LogAppointmentFee returns error? {
            ChannelingFee fee = check <- GetAppointmentFee;
            log:printInfo("ChannelingFee", payload = fee);
        }

        // @foo:DataMapper
        worker CreatePaymentRequest returns error? {
            [Appointment, ChannelingFee] [appointment, channelingFee] = check <- GetAppointmentFee;
            PaymentSettlement paymentSettlementReq = check paymentRequestPayloadMapper(requestPayload, appointment, channelingFee);
            paymentSettlementReq -> MakePayment;
        }

        // @foo:HttpPostMediator
        worker MakePayment returns error? {
            PaymentSettlement paymentSettlementReq = check <- CreatePaymentRequest;
            PaymentResponse response = check paymentEP->/.post(paymentSettlementReq);

            response -> LogPaymentResponse;
            response -> function;
        }

        // @foo:LogMediator
        worker LogPaymentResponse returns error? {
            PaymentResponse response = check <- MakePayment;
            log:printInfo("PaymentResponse", payload = response);
        }

        PaymentResponse resp = check <- MakePayment;
        return resp;
    }
}

function AppointmentPayloadMapper(ReservationRequest reservationRequest) returns AppointmentRequest => {
    patient: {
        name: reservationRequest.patient.name,
        dob: reservationRequest.patient.dob,
        ssn: reservationRequest.patient.ssn,
        address: reservationRequest.patient.address,
        phone: reservationRequest.patient.phone,
        email: reservationRequest.patient.email
    },
    doctor: reservationRequest.doctor,
    hospital: reservationRequest.hospital,
    appointment_date: reservationRequest.appointment_date
};

function paymentRequestPayloadMapper(ReservationRequest reservationReq, Appointment appointment, ChannelingFee channelingFee) returns PaymentSettlement|error => {
    appointmentNumber: appointment.appointmentNumber,
    doctor: appointment.doctor,
    patient: appointment.patient,
    fee: check decimal:fromString(channelingFee.actualFee),
    confirmed: false,
    card_number: reservationReq.patient.cardNo
};
