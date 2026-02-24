import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Reach Alert',
      theme: ThemeData(
          primarySwatch: Colors.blue,
          fontFamily: 'Poppins'
      ),
      debugShowCheckedModeBanner: false,
      home: const ConsentPage(),
    );
  }
}

final class ConsentPage extends StatefulWidget {

  const ConsentPage({super.key});

  @override
  _ConsentPageState createState() {
    return _ConsentPageState();
  }
}

class _ConsentPageState extends State<ConsentPage> {

  final MethodChannel _methodChannel = const MethodChannel('com.confegure.reach_alert/methods');
  /*var schema =
  {
    "crop_schema": {
      "data": {
        "crop": "Crop Name",
        "problem_type": "Disease/Pest",
        "caused_by": "Disease/Pest name",
        "crop_stage": "Stage of Crop",
        "season": "kharif/rabi",
        "parts_affected":
            "leaves/stems/branches/roots/fruits/flowers/vegetables",

        "id": "AUTO",
        "nature_of_damage": "TBA",
        "scientific_name": "AUTO",
        "order": "AUTO",
        "family": "AUTO",
        "ETL": "TBA",
        "measures": "TBA",
      },
      "checked_by": "Initially empty and filled after checking",
      "stage": "Uploaded/Approved/Rejected",
      "uploaded_on": "Uploaded date and time",
      "checked_on": "Checked date and time",
    },
    "user_schema": {
      "email": "Email ID",
      "name": "Name",
      "username": "User Name for login",
      "password": "Password",
      "type": "User/Approver/Admin",
      "approves": {
        "crop": "Crop Name",
        "problem": "Disease/Pest",
      },
      //Approves is empty map({}) if user is not approver.

      "id": "AUTO"
    }
  };*/

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: Container(
        color: Colors.black26,
        padding: const EdgeInsets.fromLTRB(32, 84, 32, 48),
        child: Center(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: Container(
              color: Colors.white,
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(8),
                child: Column(
                  mainAxisSize: MainAxisSize.max,
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const SizedBox(height: 24,),
                    const Text("Background Location Access", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: Colors.blue)),
                    const SizedBox(height: 36,),
                    RichText(
                      text: const TextSpan(
                          style: TextStyle(
                              fontFamily: "Poppins",
                              fontSize: 16,
                              color: Colors.black
                          ),
                          children: [
                            TextSpan(
                              text: "Reach Alert ",
                              style: TextStyle(fontWeight: FontWeight.bold),
                            ),
                            TextSpan(
                                text: "collects location data to enable calculating the distance to your destination, "
                                    "alert you as soon as you reach your destination, even when the app is closed or "
                                    "not in use. \nThis data is not shared to any third-party services. We use your data purely for the application."
                            ),
                            TextSpan(
                                text: "\n\nFor example, when you set a destination we start collecting your current location data and calculate the distance from your current location to your destination. For this feature to work in the background or when app is not in use, we need background location access. ",
                                style: TextStyle(fontSize: 10, fontWeight: FontWeight.w100)
                            ),
                          ]
                      ),
                    ),
                    const SizedBox(height: 64,),
                    Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          TextButton(onPressed: (){
                            _methodChannel.invokeMethod('locationPermissionResponse', <String, dynamic>{"accepted": false});
                          }, child: const Text("Decline", style: TextStyle(color: Colors.deepOrange),)),
                          ElevatedButton(onPressed: (){
                            _methodChannel.invokeMethod('locationPermissionResponse', <String, dynamic>{"accepted": true});
                          }, child: const Text("Accept", style: TextStyle(color: Colors.white),))
                        ],
                      ),
                    )
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
