package org.firstinspires.ftc.teamcode.Teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.SubSystems.Robot;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

@TeleOp(name = "Multithreading Test")
public class MultithreadingTest extends LinearOpMode {
    //Declare DC motor objects
    private Robot robot;

    double mainArmHorizontalPos = 0.0;
    double mainArmVerticalPos = 0.0;
    double mainArmHorizontalMax = 1000.0;
    double mainArmVerticalMax = 1100.0;
    double mainArmIncrement = 600.0;
    double mainClawRotationAngle;
    double mainClawRotationIncrement = 300;
    double deltaT;
    double timeCurrent;
    double timePre;
    ElapsedTime timer;

    private boolean mainClawArmControlDigital = true;
    private boolean mainClawArmDeployed = false;
    private boolean csClawArmControlDigital = true;
    private boolean csClawArmDeployed = false;

    enum Prospective {
        ROBOT,
        DRIVER,
    }

    enum MainClawState {
        CLOSE,
        OPEN,
        WIDEOPEN,
    }
    private MainClawState mainClawState;

    private Prospective prospectiveMode = Prospective.ROBOT;
    private double robotAngle;
    private boolean visionEnabled = false;

    private int verticalStepsCount = 10;
    private double[] verticalSteps = {110.0, 210.0, 310.0, 410.0, 510.0, 603.0, 693.0, 790.0, 880.0, 970.0};

    private void initOpMode() {
        //Initialize DC motor objects
        timer = new ElapsedTime();
        if (visionEnabled) {
            // visionMode 4: backWebcam is initialized for Vuforia and armWebcam is initialized for OpenCV
            try {
                robot = new Robot(this, timer, 4);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                robot = new Robot(this, timer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        timeCurrent = timer.nanoseconds();
        timePre = timeCurrent;

        telemetry.addData("Wait for start", "");
        telemetry.update();
    }

    @Override
    public void runOpMode() throws InterruptedException {
        initOpMode();

        Thread deployMainClawArm = new DeployMainClawArm();
        Thread visionThread = new VisionThread();
        Thread driveThread = new DriveThread();
        
        waitForStart();

        // call initServosTeleop() after running Auto program
        robot.initServosTeleop();
        // call initServosAuto() if testing Teleop stand-alone
//        robot.initServosAuto();
        mainClawRotationAngle = robot.control.getMainClawRotationDegrees();
        telemetry.clearAll();
        timeCurrent = timer.nanoseconds();
        timePre = timeCurrent;
        if (visionEnabled) {
            robot.vision.getTargetsSkyStone().activate();
        }
        robot.control.closeMainClawStone();
        mainClawState = MainClawState.CLOSE;


        while(opModeIsActive()) {

            // Get gamepad inputs
            robot.getGamePadInputs();

            // Get the current time
            timeCurrent = timer.nanoseconds();
            deltaT = timeCurrent - timePre;
            timePre = timeCurrent;

            if (visionEnabled) {
                robot.vision.vuMarkScan();
            }

            driveThread.start();
            visionThread.start();

            moveFoundationClaw();

            deployMainClawArm.start();

            // deploy capstone claw arm
            if (csClawArmControlDigital) {
                if (robot.bumperLeft2 && !robot.islBumper2PressedPrev) { // toggle capstone claw arm deploy mode
                    if (csClawArmDeployed) {
                        robot.control.retractCSClawArm();
                        csClawArmDeployed = false;
                    }
                    else {
                        robot.control.setCSClawArmDegrees(robot.control.getMainArmTargetAngle());
                        csClawArmDeployed = true;
                    }
                }
            }

            // control main claw
            if ((robot.triggerLeft2 > 0.5) && (robot.triggerRight2 < 0.5)) { // main claw open
//                if ((mainClawState == MainClawState.OPEN) || (mainClawState == MainClawState.WIDEOPEN)) {
//                    robot.control.openMainClawWide();
//                    mainClawState = MainClawState.WIDEOPEN;
//                }
//                else {
//                    robot.control.openMainClaw();
//                    mainClawState = MainClawState.OPEN;
//                }
                robot.control.openMainClaw();
            } else if ((robot.triggerRight2 > 0.5) && (robot.triggerLeft2 < 0.5)) { // main claw close
//                if (mainClawState == MainClawState.WIDEOPEN) {
//                    robot.control.openMainClaw();
//                    mainClawState = MainClawState.OPEN;
//                }
//                else {
//                    robot.control.closeMainClawStone();
//                    mainClawState = MainClawState.CLOSE;
//                }
                robot.control.closeMainClawStone();
            }

            // control capstone claw
            if (robot.yButton2 && (!robot.aButton2)) { // capstone claw open
                robot.control.openCSClaw();
            } else if (robot.aButton2 && (!robot.yButton2)) { // capstone claw close
                robot.control.closeCSClaw();
            }

            // move robot main arm
            // move robot main arm along horizontal line
            if(robot.leftStickY2 >= 0.1){
                mainArmHorizontalPos = mainArmHorizontalPos + (robot.leftStickY2 - 0.1) * (robot.leftStickY2 - 0.1) * mainArmIncrement * deltaT/1e9;
            }
            else if(robot.leftStickY2  <= -0.1){
                mainArmHorizontalPos = mainArmHorizontalPos - (robot.leftStickY2 + 0.1) * (robot.leftStickY2 + 0.1) * mainArmIncrement * deltaT/1e9;
            }
            if (mainArmHorizontalPos > mainArmHorizontalMax) {
                mainArmHorizontalPos = mainArmHorizontalMax;
            }
            if (mainArmHorizontalPos < 0.0) {
                mainArmHorizontalPos =0.0;
            }
            // move robot main arm along vertical line
            if(robot.rightStickY2 >= 0.1){
                mainArmVerticalPos = mainArmVerticalPos + (robot.rightStickY2 - 0.1) * (robot.rightStickY2 - 0.1) * mainArmIncrement * deltaT/1e9;
            }
            else if(robot.rightStickY2  <= -0.1){
                mainArmVerticalPos = mainArmVerticalPos - (robot.rightStickY2 + 0.1) * (robot.rightStickY2 + 0.1) * mainArmIncrement * deltaT/1e9;
            }
            if (mainArmVerticalPos > mainArmVerticalMax) {
                mainArmVerticalPos = mainArmVerticalMax;
            }
            if (mainArmVerticalPos < 0.0) {
                mainArmVerticalPos =0.0;
            }
            robot.control.setMainArmPosition(mainArmHorizontalPos, mainArmVerticalPos);

            // rotate main claw
            if(robot.rightStickX2 >= 0.1){
                mainClawRotationAngle = mainClawRotationAngle + (robot.rightStickX2 - 0.1) * (robot.rightStickX2 - 0.1) * mainClawRotationIncrement * deltaT/1e9;
            }
            else if(robot.rightStickX2  <= -0.1){
                mainClawRotationAngle = mainClawRotationAngle - (robot.rightStickX2 + 0.1) * (robot.rightStickX2 + 0.1) * mainClawRotationIncrement * deltaT/1e9;
            }
            if (mainClawRotationAngle > 180.0) {
                mainClawRotationAngle = 180.0;
            }
            if (mainClawRotationAngle < 0.0) {
                mainClawRotationAngle =0.0;
            }
            robot.control.setMainClawRotationDegrees(mainClawRotationAngle);

            if(mainClawArmControlDigital && mainClawArmDeployed){
                robot.control.setMainClawArmDegrees(robot.control.getMainArmTargetAngle());
            }
            if(csClawArmDeployed && csClawArmControlDigital){
                robot.control.setCSClawArmDegrees(robot.control.getMainArmTargetAngle());
            }

            //Automate skybrige pos
            if(robot.bButton2 && !robot.isbButton2PressedPrev){
                mainArmHorizontalPos = 40.0;
                mainArmVerticalPos = 50.0;
                robot.control.setMainArmPosition(mainArmHorizontalPos, mainArmVerticalPos);
            }

            //Automate stone grabbing
            if(robot.xButton2 && !robot.isxButton2PressedPrev){
                if (mainArmVerticalPos < 150.0) { // only do this when the arm is low to prevent accidents
                    // make sure main claw is open
                    robot.control.openMainClaw();

                    // move main arm down
                    mainArmVerticalPos = 0.0;
                    robot.control.setMainArmPosition(mainArmHorizontalPos, mainArmVerticalPos);
                    robot.control.setMainClawArmDegrees(robot.control.getMainArmTargetAngle());
                    sleep (200);
                    robot.control.closeMainClawStone();
                    sleep (400);
                    mainArmVerticalPos = 50.0;
                    robot.control.setMainArmPosition(mainArmHorizontalPos, mainArmVerticalPos);
                }
            }

            //Automate stone vertical stepping
            if(robot.dPadUp2 && !robot.isdPadUp2PressedPrev){
                int currentStep = 0;
                while ((currentStep < verticalStepsCount) && (mainArmVerticalPos + 15.0 > verticalSteps[currentStep])) {
                    currentStep = currentStep + 1;
                }
                if (currentStep < verticalStepsCount) {
                    mainArmVerticalPos = verticalSteps[currentStep];
                    robot.control.setMainArmPosition(mainArmHorizontalPos, mainArmVerticalPos);
                }
            }

            //Automate stone vertical stepping
            if(robot.dPadDown2 && !robot.isdPadDown2PressedPrev){
                int currentStep = verticalStepsCount - 1;
                while ((currentStep >= 0) && (mainArmVerticalPos - 15.0 < verticalSteps[currentStep])) {
                    currentStep = currentStep - 1;
                }
                if (currentStep >= 0) {
                    mainArmVerticalPos = verticalSteps[currentStep];
                    robot.control.setMainArmPosition(mainArmHorizontalPos, mainArmVerticalPos);
                }
            }

            // reset drive motor encoders
            if (robot.yButton && !robot.isyButtonPressedPrev) {
                robot.drive.resetDriveMotorEncoders();
            }

            // toggle drive prospective mode
            if (robot.xButton && !robot.isxButtonPressedPrev) {
                if (prospectiveMode == Prospective.DRIVER) {
                    prospectiveMode = Prospective.ROBOT;
                }
                else {
                    prospectiveMode = Prospective.DRIVER;
                }
            }

            telemetry.addData("Arm ","X %.1f, Y %.1f", mainArmHorizontalPos, mainArmVerticalPos);
            telemetry.addData("Arm "," tilt %.0f, %.0f; length %.0f, %.0f",
                    robot.control.getMainArmAngleTickTarget(), robot.control.getMainArmAngleTickCurrent(),
                    robot.control.getMainArmLengthTickTarget(), robot.control.getMainArmLengthTickCurrent());
            telemetry.addData("clawRotation", mainClawRotationAngle);
            telemetry.addData("Drive Mode ", prospectiveMode.toString());
            telemetry.addData("robot angle ", robotAngle);
            int currentPositions[] = robot.drive.getCurrentPositions();
            telemetry.addData("position", "fl %d, fr %d, rl %d, rr %d",
                    currentPositions[0], currentPositions[1], currentPositions[2], currentPositions[3]);
//            telemetry.addData("Right Rear Power", robot.rearRightDriveMotor.getPower());
//            telemetry.addData("Right Front Power", robot.frontRightDriveMotor.getPower());

            telemetry.update();
        }
    }

    public void moveFoundationClaw() {
        if (robot.bumperLeft && (!robot.bumperRight)) { // foundation claw up
            robot.control.raiseClawsFromFoundation();
        } else if (robot.bumperRight && (!robot.bumperLeft)) { // foundation claw down
            robot.control.lowerClawsToFoundation();
        }
        if ((robot.triggerLeft > 0.5) && (robot.triggerRight < 0.5)) { // foundation claw up
            robot.control.raiseClawsFromFoundation();
        } else if ((robot.triggerRight > 0.5) && (robot.triggerLeft < 0.5)) { // foundation claw down
            robot.control.lowerClawsToFoundation();
        }
    }

    class DeployMainClawArm extends Thread {
        public DeployMainClawArm() {
            this.setName("Deploy Main Claw Arm");

            telemetry.addData("Deploy Main Claw Arm ", this.getName());
            telemetry.update();
        }

        // called when tread.start is called. thread stays in loop to do what it does until exit is
        // signaled by main code calling thread.interrupt.
        @Override
        public void run() {
            telemetry.addData("Starting thread ", this.getName());
            int evenValue = 0;
            try {
//                while (!isInterrupted()) {
//                    // we record the Y values in the main class to make showing them in telemetry
//                    // easier.
//
//                    robot.leftStickY = gamepad1.left_stick_y * -1;
//                    robot.rightStickY = gamepad1.right_stick_y * -1;
//
////                    robot.frontLeftDriveMotor.setPower(Range.clip(leftY, -1.0, 1.0));
////                    rightMotor.setPower(Range.clip(rightY, -1.0, 1.0));
//
//                    idle();
//                }

                while (!isInterrupted()) {
//                    telemetry.addData("Running thread ",evenValue);
//                    telemetry.update();
                    if (mainClawArmControlDigital) {
                        if (robot.bumperRight2 && !robot.isrBumper2PressedPrev) { // toggle main claw arm deploy mode
                            if (mainClawArmDeployed) {
                                robot.control.retractMainClawArm();
                                mainClawArmDeployed = false;
                            }
                            else {
                                robot.control.setMainClawArmDegrees(robot.control.getMainArmTargetAngle());
                                mainClawArmDeployed = true;
                            }
                        }
                    }

                }

                evenValue += 2;
            }
            // interrupted means time to shutdown. note we can stop by detecting isInterrupted = true
            // or by the interrupted exception thrown from the sleep function.
            // an error occurred in the run loop.
            catch (Exception e) {
//                e.printStackTrace(Logging.logPrintStream);
            }

//            Logging.log("end of thread %s", this.getName());
        }
    }


    class VisionThread extends Thread {
        public VisionThread() {
            this.setName("VisionThread");

            telemetry.addData("VisionThread ", this.getName());
            telemetry.update();
        }

        // called when tread.start is called. thread stays in loop to do what it does until exit is
        // signaled by main code calling thread.interrupt.
        @Override
        public void run() {
            telemetry.addData("Starting thread ", this.getName());
            int evenValue = 0;
            try {
//                while (!isInterrupted()) {
//                    // we record the Y values in the main class to make showing them in telemetry
//                    // easier.
//
//                    robot.leftStickY = gamepad1.left_stick_y * -1;
//                    robot.rightStickY = gamepad1.right_stick_y * -1;
//
////                    robot.frontLeftDriveMotor.setPower(Range.clip(leftY, -1.0, 1.0));
////                    rightMotor.setPower(Range.clip(rightY, -1.0, 1.0));
//
//                    idle();
//                }

                while (!isInterrupted()) {
                    timeCurrent = timer.nanoseconds();
                    timePre = timeCurrent;

                    robot.vision.getTargetsSkyStone().activate();

                    mainArmHorizontalPos = 40.0;
                    mainArmVerticalPos = 80.0;
                    robot.control.setMainArmPosition(mainArmHorizontalPos, mainArmVerticalPos);
                    robot.control.setMainClawArmDegrees(robot.control.getMainArmTargetAngle());

                    // save images
                    if (robot.aButton && !robot.isaButtonPressedPrev) {
                        robot.getOpmode().telemetry.addData("saving ", "images...");
                        robot.vision.saveImage("VisionTest", robot.vision.frameBuffer2, Imgproc.COLOR_RGBA2BGR, "original", (long) timeCurrent);
                        robot.vision.saveImage("VisionTest", robot.vision.frameBuffer1, Imgproc.COLOR_RGBA2BGR, "undistorted", (long) timeCurrent);
                        robot.vision.saveImage("VisionTest", robot.vision.yCbCrChan2Mat_compensatedn, Imgproc.COLOR_RGBA2BGR, "CbImage_c25n", (long) timeCurrent);
                        robot.vision.saveImage("VisionTest", robot.vision.yCbCrChan2Mat_compensated, Imgproc.COLOR_RGBA2BGR, "CbImage_c25", (long) timeCurrent);
                        robot.vision.saveImage("VisionTest", robot.vision.yCbCrChan1Mat_compensated, Imgproc.COLOR_RGBA2BGR, "CrImage_c25", (long) timeCurrent);
                        robot.vision.saveImage("VisionTest", robot.vision.yCbCrChan2Mat, Imgproc.COLOR_RGBA2BGR, "CbImage", (long) timeCurrent);
                        robot.vision.saveImage("VisionTest", robot.vision.yCbCrChan1Mat, Imgproc.COLOR_RGBA2BGR, "CrImage", (long) timeCurrent);
                        robot.vision.saveImage("VisionTest", robot.vision.yCbCrChan0Mat, Imgproc.COLOR_RGBA2BGR, "YImage", (long) timeCurrent);
                        robot.vision.saveImage("VisionTest", robot.vision.thresholdMat, Imgproc.COLOR_RGBA2BGR, "threshold", (long) timeCurrent);
                        robot.vision.saveImage("VisionTest", robot.vision.contoursOnFrameMat, Imgproc.COLOR_RGBA2BGR, "contours", (long) timeCurrent);
                    }
                }

            }
            // interrupted means time to shutdown. note we can stop by detecting isInterrupted = true
            // or by the interrupted exception thrown from the sleep function.
            // an error occurred in the run loop.
            catch (Exception e) {
//                e.printStackTrace(Logging.logPrintStream);
            }

//            Logging.log("end of thread %s", this.getName());
        }
    }




    class DriveThread extends Thread {
        public DriveThread() {
            this.setName("DriveThread");

            telemetry.addData("DriveThread ", this.getName());
            telemetry.update();
        }

        // called when tread.start is called. thread stays in loop to do what it does until exit is
        // signaled by main code calling thread.interrupt.
        @Override
        public void run() {
            telemetry.addData("Starting thread ", this.getName());
            int evenValue = 0;
            try {
//                while (!isInterrupted()) {
//                    // we record the Y values in the main class to make showing them in telemetry
//                    // easier.
//
//                    robot.leftStickY = gamepad1.left_stick_y * -1;
//                    robot.rightStickY = gamepad1.right_stick_y * -1;
//
////                    robot.frontLeftDriveMotor.setPower(Range.clip(leftY, -1.0, 1.0));
////                    rightMotor.setPower(Range.clip(rightY, -1.0, 1.0));
//
//                    idle();
//                }

                while (!isInterrupted()) {

                    double[] motorPowers;
                    robotAngle = robot.imu.getAngularOrientation().firstAngle;
                    if (prospectiveMode == Prospective.ROBOT) {
                        motorPowers = robot.drive.calcMotorPowers(robot.leftStickX, robot.leftStickY, robot.rightStickX);
                    }
                    else {  // DRIVER prospective mode
                        // Get robot angle
                        double relativeX = robot.leftStickX * Math.cos(robotAngle*Math.PI/180.0) + robot.leftStickY * Math.sin(robotAngle*Math.PI/180.0);
                        double relativeY = -robot.leftStickX * Math.sin(robotAngle*Math.PI/180.0) + robot.leftStickY * Math.cos(robotAngle*Math.PI/180.0);
                        motorPowers = robot.drive.calcMotorPowers(relativeX, relativeY, robot.rightStickX);
                    }
                    robot.drive.setDrivePowers(motorPowers);
                }

            }
            // interrupted means time to shutdown. note we can stop by detecting isInterrupted = true
            // or by the interrupted exception thrown from the sleep function.
            // an error occurred in the run loop.
            catch (Exception e) {
//                e.printStackTrace(Logging.logPrintStream);
            }

//            Logging.log("end of thread %s", this.getName());
        }
    }

}
