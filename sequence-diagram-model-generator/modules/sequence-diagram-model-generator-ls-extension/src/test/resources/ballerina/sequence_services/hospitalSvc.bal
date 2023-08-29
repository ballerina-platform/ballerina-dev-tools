import ballerina/http;
import ballerina/lang.regexp;
import ballerina/time;
import ballerina/uuid;

type Doctor record {|
    readonly string name;
    string hospital;
    string category;
    string availability;
    decimal fee;
|};

type Patient record {|
    string name;
    string dob;
    readonly string ssn;
    string address;
    string phone;
    string email;
|};

type Appointment record {|
    string time?; // todo
    readonly int appointmentNumber;
    Doctor doctor;
    Patient patient;
    string hospital;
    decimal fee;
    boolean confirmed;
    string paymentID?; // todo
    string appointmentDate;
|};

type AppointmentRequest record {|
    Patient patient;
    string doctor;
    string hospital;
    string appointment_date;
|};

type PatientRecord record {|
    Patient patient;
    map<string[]> symptoms = {};
    map<string[]> treatments = {};
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

type Payment record {|
    int appointmentNo;
    string doctorName;
    string patient;
    decimal actualFee;
    int discount;
    decimal discounted;
    string paymentID = uuid:createType4AsString();
    string status;
|};

type Status record {|
    string status;
|};

isolated string[] categories = [
    "surgery",
    "cardiology",
    "gynaecology",
    "ent",
    "paediatric"
];

isolated table<Doctor & readonly> key(name) doctors = table [
    {name: "thomas collins", hospital: "grand oak community hospital", category: "surgery", availability: "9.00 a.m - 11.00 a.m", fee: 7000},
    {name: "henry parker", hospital: "grand oak community hospital", category: "ent", availability: "9.00 a.m - 11.00 a.m", fee: 4500},
    {name: "abner jones", hospital: "grand oak community hospital", category: "gynaecology", availability: "8.00 a.m - 10.00 a.m", fee: 11000},
    {name: "joy wilson", hospital: "grand oak community hospital", category: "ent", availability: "8.00 a.m - 10.00 a.m", fee: 6750}, // changed
    {name: "anne clement", hospital: "clemency medical center", category: "surgery", availability: "8.00 a.m - 10.00 a.m", fee: 12000},
    {name: "thomas kirk", hospital: "clemency medical center", category: "gynaecology", availability: "9.00 a.m - 11.00 a.m", fee: 8000},
    {name: "cailen cooper", hospital: "clemency medical center", category: "paediatric", availability: "9.00 a.m - 11.00 a.m", fee: 5500},
    {name: "seth mears", hospital: "pine valley community hospital", category: "surgery", availability: "3.00 p.m - 5.00 p.m", fee: 8000},
    {name: "emeline fulton", hospital: "pine valley community hospital", category: "cardiology", availability: "8.00 a.m - 10.00 a.m", fee: 4000},
    {name: "jared morris", hospital: "willow gardens general hospital", category: "cardiology", availability: "9.00 a.m - 11.00 a.m", fee: 10000},
    {name: "henry foster", hospital: "willow gardens general hospital", category: "paediatric", availability: "8.00 a.m - 10.00 a.m", fee: 10000}
];

isolated table<Appointment> key(appointmentNumber) appointments = table [];

isolated table<Patient> key(ssn) patients = table [];

isolated map<PatientRecord> patientRecords = {};

isolated table<Payment & readonly> key (paymentID) payments = table [];

isolated int appointmentNumber = 1;

listener http:Listener ln = new (9090);

service /healthcare on ln {
    resource function get [string category]() returns Doctor[]|http:NotFound {
        Doctor[] & readonly doctors = getDoctorsByCategory(category);
        if doctors.length() == 0 {
            return {body: "no doctors found for specified category"};
        }
        return doctors;
    }

    resource function get appointments/[int appointment_id]()
        returns (Appointment & readonly)|http:NotFound => getAppointment(appointment_id);

    resource function get appointments/validity/[int appointment_id]() returns Status|http:NotFound {
        final string appointmentDate;
        lock {
            if !appointments.hasKey(appointment_id) {
                return {body: "unknown appointment ID"};
            }
            do {
            E();
            } on fail error err {
                return {body: err.message};
            }
            appointmentDate = appointments.get(appointment_id).appointmentDate;
            Doctor[] & readonly doctors = getDoctorsByCategory(category);
        }
        // todo period calculation
        return {status: "1"};
    }

    resource function delete appointments/[int appointment_id]() returns Status|http:NotFound {
        lock {
            if appointments.hasKey(appointment_id) {
                _ = appointments.remove(appointment_id);
                return {status: "appointment removed successfully"};
            }
        }
        return {body: "unknown appointment ID"};
    }

    resource function post payments(PaymentSettlement & readonly paymentSettlement) returns Payment|http:NotFound|error {
        lock {
            if !appointments.hasKey(paymentSettlement.appointmentNumber) {
                return {body: "unknown appointment ID"};
            }
        }
        PaymentSettlement {
            patient: {name: patientName, dob},
            doctor: {name: doctorName},
            appointmentNumber
        } = paymentSettlement;
        int discount = let int age = check getAge(dob) in
                                age < 12 ? 15 : (age > 55 ? 20 : 0);
        Doctor {fee} = check getDoctor(doctorName).ensureType();
        final Payment & readonly payment = {
            discounted: (100 - discount) * fee / 100,
            doctorName,
            patient: patientName,
            discount,
            appointmentNo: appointmentNumber,
            actualFee: fee,
            status: "settled"
        };
        lock {
            payments.put(payment);
        }
        return payment;
    }

    resource function get payments() returns Payment[] {
        lock {
            return <readonly> from Payment payment in payments select payment;
        }
    }

    resource function get payments/payment/[string payment_id]() returns Payment|http:NotFound {
        lock {
            if payments.hasKey(payment_id) {
                return payments.get(payment_id);
            }
        }
        return {body: "unknown payment ID"};
    }

    resource function post admin/newdoctor(Doctor & readonly doctor)
            returns Status|http:NotFound => addDoctor(doctor);
}

service on ln {
    resource function post [string hospital]/categories/[string category]/reserve(AppointmentRequest & readonly appointmentRequest)
            returns Appointment|http:NotFound {
        lock {
            if !categories.some(availableCategory => category == availableCategory) {
                return {body: "invalid category"};
            }
        }

        (Doctor & readonly)? doctor = getDoctor(appointmentRequest.doctor);
        final string requestedHospital = appointmentRequest.hospital;
        if doctor is () || doctor.hospital != requestedHospital {
            return {body: "requested doctor is not available at the requested hospital"};
        }

        int newAppointmentNumber;

        lock {
            newAppointmentNumber = appointmentNumber;
            appointmentNumber += 1;
        }

        final Patient & readonly patient = appointmentRequest.patient;
        final Appointment & readonly appointment = {
            appointmentNumber: newAppointmentNumber,
            doctor,
            patient,
            fee: doctor.fee,
            confirmed: false,
            appointmentDate: appointmentRequest.appointment_date,
            hospital: requestedHospital
        };

        lock {
            appointments.put(appointment);
        }

        lock {
            patients.put(patient);
        }

        lock {
            string ssn = patient.ssn;
            if !patientRecords.hasKey(ssn) {
                patientRecords[ssn] = {patient};
            }
        }
        return appointment;
    }

    resource function get [string hospital]/categories/appointments/[int appointment_id]()
        returns Appointment|http:NotFound => getAppointment(appointment_id);

    resource function get [string hospital]/categories/appointments/[int appointment_id]/fee()
            returns ChannelingFee|http:NotFound {
        lock {
            if appointments.hasKey(appointment_id) {
                Appointment {doctor: {name: doctorName, fee}, patient: {name: patientName}} = appointments.get(appointment_id);
                return {
                    patientName: patientName.toLowerAscii(),
                    doctorName: doctorName.toLowerAscii(),
                    actualFee: fee.toString()
                };
            }
        }
        return {body: "unknown appointment ID"};
    }

    resource function post [string hospital]/categories/patient/updaterecord(
            record {|string SSN; string[] symptoms; string[] treatments;|} & readonly patientDetails)
            returns Status|http:NotFound {
        var {SSN: ssn, symptoms, treatments} = patientDetails;
        lock {
            if !patients.hasKey(ssn) {
                return {body: "unknown patient"};
            }
        }

        lock {
            PatientRecord {symptoms: symptomsRecord, treatments: treatmentsRecord} = patientRecords.get(ssn);
            string time = regexp:split(re `T`, time:utcToString(time:utcNow()))[0];
            symptomsRecord[time] = symptoms;
            treatmentsRecord[time] = treatments;
        }
        return {status: "update successful"};
    }

    resource function get [string hospital]/categories/patient/[string SSN]/getrecord()
            returns PatientRecord|http:NotFound {
        lock {
            if patientRecords.hasKey(SSN) {
                return patientRecords.get(SSN).clone();
            }
        }
        return {body: "unknown patient entry"};
    }

    resource function get [string hospital]/categories/patient/appointment/[int appointment_id]/discount()
            returns Status|http:NotFound|error {
        lock {
            if appointments.hasKey(appointment_id) {
                int age = check getAge(appointments.get(appointment_id).patient.dob);
                return {status: (age < 12 || age > 55).toString()};
            }
        } on fail error err {
           E();
        }
        return {body: "unknown appointment ID"};
    }

    resource function post [string hospital]/categories/admin/doctor/newdoctor(Doctor & readonly doctor)
            returns Status|http:NotFound => addDoctor(doctor);
}


function E(){
    return 1;
}

isolated function getAge(string dob) returns int|error {
    string yob = regexp:split(re `-`, dob)[0];
    string currYear = regexp:split(re `-`, time:utcToString(time:utcNow()))[0];
    return (check int:fromString(currYear) - check int:fromString(yob));
}

isolated function getDoctor(string name) returns (Doctor & readonly)? {
    lock {
        return doctors.hasKey(name) ? doctors.get(name) : ();
    }
}

isolated function getDoctorsByCategory(string category) returns Doctor[] & readonly {
    lock {
        return from Doctor & readonly doctor in doctors
                where doctor.category == category
                select doctor;
    }
}

isolated function getAppointment(int appointmentId) returns (Appointment & readonly)|http:NotFound {
    lock {
        if appointments.hasKey(appointmentId) {
            return appointments.get(appointmentId).cloneReadOnly();
        }
    }
    return {body: "unknown appointment ID"};
}

isolated function addDoctor(Doctor & readonly doctor) returns Status|http:NotFound {
    final var {name, category} = doctor;
    lock {
        // if !categories.some(existingCategory => existingCategory == category) { // crashes
        if categories.indexOf(category) is () {
            categories.push(category);
        }
    }


    lock {
        if getDoctor(name) !is () {
            return {status: "doctor already exists in the system"};
        }
        doctors.put(doctor);
    }

    return {status: "doctor added successfully"};
}