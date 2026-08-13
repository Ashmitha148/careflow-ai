import { useState, useRef, useEffect } from "react";
import {
  Video,
  Camera,
  RotateCcw,
  CheckCircle,
  AlertCircle,
  Mic,
  MicOff,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";

const API_BASE = "/api";

export default function VideoVerificationPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [step, setStep] = useState("select"); // select, camera, recording, review, submitting, success, error
  const [stream, setStream] = useState(null);
  const [recordedBlob, setRecordedBlob] = useState(null);
  const [videoUrl, setVideoUrl] = useState(null);
  const [selectedMedication, setSelectedMedication] = useState("");
  const [medications, setMedications] = useState([]);
  const [recordingTime, setRecordingTime] = useState(0);
  const [error, setError] = useState(null);
  const [includeAudio, setIncludeAudio] = useState(true);

  const videoRef = useRef(null);
  const mediaRecorderRef = useRef(null);
  const timerRef = useRef(null);
  const chunksRef = useRef([]);

  // Load patient's important medications
  useEffect(() => {
    const token = localStorage.getItem("careflow_token");
    fetch(`${API_BASE}/patients/my`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((patients) => {
        if (patients.length > 0) {
          return fetch(`${API_BASE}/clinical/medications/${patients[0].id}`, {
            headers: { Authorization: `Bearer ${token}` },
          });
        }
        throw new Error("No patient record found");
      })
      .then((r) => r.json())
      .then((data) => {
        const important =
          data?.filter((m) => m.important && m.status === "ACTIVE") || [];
        setMedications(important);
      })
      .catch((err) => setError(err.message));
  }, []);

  const startCamera = async () => {
    try {
      const constraints = {
        video: {
          facingMode: "user",
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
        audio: includeAudio,
      };
      const mediaStream =
        await navigator.mediaDevices.getUserMedia(constraints);
      setStream(mediaStream);
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
      }
      setStep("camera");
      setError(null);
    } catch (err) {
      setError(
        "Could not access camera. Please allow camera permissions in your browser.",
      );
    }
  };

  const startRecording = () => {
    if (!stream) return;
    chunksRef.current = [];
    const options = { mimeType: "video/webm;codecs=vp9,opus" };
    const mediaRecorder = new MediaRecorder(stream, options);
    mediaRecorderRef.current = mediaRecorder;

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunksRef.current.push(e.data);
    };

    mediaRecorder.onstop = () => {
      const blob = new Blob(chunksRef.current, { type: "video/webm" });
      setRecordedBlob(blob);
      setVideoUrl(URL.createObjectURL(blob));
      setStep("review");
      clearInterval(timerRef.current);
      setRecordingTime(0);
    };

    mediaRecorder.start(1000); // Collect data every second
    setStep("recording");
    setRecordingTime(0);

    timerRef.current = setInterval(() => {
      setRecordingTime((t) => {
        if (t >= 29) {
          stopRecording();
          return t;
        }
        return t + 1;
      });
    }, 1000);
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current?.state === "recording") {
      mediaRecorderRef.current.stop();
    }
    stream?.getTracks().forEach((track) => track.stop());
    setStream(null);
    clearInterval(timerRef.current);
  };

  const retake = () => {
    setRecordedBlob(null);
    setVideoUrl(null);
    setStep("select");
    setRecordingTime(0);
  };

  const submitVerification = async () => {
    if (!recordedBlob || !selectedMedication) return;
    setStep("submitting");

    const formData = new FormData();
    formData.append("video", recordedBlob, "verification.webm");
    formData.append("userId", user.id);
    formData.append("status", "GIVEN");
    formData.append(
      "notes",
      "Remote video verification submitted by patient via browser",
    );

    const token = localStorage.getItem("careflow_token");
    try {
      const response = await fetch(
        `${API_BASE}/clinical/medications/${selectedMedication}/administrations/verify`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
          body: formData,
        },
      );

      if (response.ok) {
        setStep("success");
      } else {
        const err = await response.text();
        throw new Error(err || "Submission failed");
      }
    } catch (err) {
      setStep("error");
      setError(
        err.message || "Failed to submit verification. Please try again.",
      );
    }
  };

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, "0")}`;
  };

  return (
    <div className="space-y-6 pb-10 max-w-2xl mx-auto">
      <div>
        <p className="text-xs font-medium text-teal-400">Remote Verification</p>
        <h1 className="mt-1 text-xl font-semibold">Medication Verification</h1>
        <p className="mt-1 text-xs text-[var(--text-muted)]">
          Record a short video to confirm you took your medication
        </p>
      </div>

      {step === "select" && (
        <div className="surface rounded-2xl p-6 space-y-4">
          <div className="flex items-center gap-3">
            <Video className="h-5 w-5 text-teal-400" />
            <h2 className="text-sm font-medium">Select Medication</h2>
          </div>

          {medications.length === 0 ? (
            <div className="text-center py-8">
              <CheckCircle className="mx-auto h-8 w-8 text-emerald-400" />
              <p className="mt-3 text-sm font-medium">
                No medications require verification
              </p>
              <p className="mt-1 text-xs text-[var(--text-muted)]">
                All your important medications are up to date.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {medications.map((med) => (
                <button
                  key={med.id}
                  onClick={() => setSelectedMedication(med.id)}
                  className={`w-full flex items-center justify-between p-3 rounded-xl border transition ${
                    selectedMedication === med.id
                      ? "border-teal-500 bg-teal-500/10"
                      : "border-[var(--border-color)] hover:bg-[var(--bg-hover)]"
                  }`}
                >
                  <div className="text-left">
                    <p className="text-sm font-medium">{med.name}</p>
                    <p className="text-xs text-[var(--text-muted)]">
                      {med.dosage} · {med.frequency}
                    </p>
                  </div>
                  {selectedMedication === med.id && (
                    <CheckCircle className="h-4 w-4 text-teal-400" />
                  )}
                </button>
              ))}
            </div>
          )}

          {selectedMedication && (
            <div className="space-y-3 pt-2">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setIncludeAudio(!includeAudio)}
                  className="flex items-center gap-2 text-xs text-[var(--text-muted)] hover:text-[var(--text-primary)]"
                >
                  {includeAudio ? (
                    <Mic className="h-3 w-3" />
                  ) : (
                    <MicOff className="h-3 w-3" />
                  )}
                  {includeAudio ? "Audio enabled" : "Audio disabled"}
                </button>
              </div>
              <button
                onClick={startCamera}
                className="w-full flex items-center justify-center gap-2 rounded-xl bg-teal-500 px-4 py-3 text-sm font-semibold text-slate-950 hover:bg-teal-400 transition"
              >
                <Camera className="h-4 w-4" />
                Start Camera
              </button>
            </div>
          )}
        </div>
      )}

      {(step === "camera" || step === "recording") && (
        <div className="surface rounded-2xl p-6 space-y-4">
          <div className="relative aspect-video bg-black rounded-xl overflow-hidden">
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className="w-full h-full object-cover"
            />
            {step === "recording" && (
              <div className="absolute top-4 left-4 flex items-center gap-2 bg-black/60 rounded-lg px-3 py-1">
                <div className="h-2 w-2 rounded-full bg-rose-500 animate-pulse" />
                <span className="text-xs text-white font-mono">
                  {formatTime(recordingTime)} / 0:30
                </span>
              </div>
            )}
          </div>

          <div className="flex gap-3">
            {step === "camera" ? (
              <button
                onClick={startRecording}
                className="flex-1 flex items-center justify-center gap-2 rounded-xl bg-rose-500 px-4 py-3 text-sm font-semibold text-white hover:bg-rose-400 transition"
              >
                <div className="h-3 w-3 rounded-full bg-white" />
                Start Recording
              </button>
            ) : (
              <button
                onClick={stopRecording}
                className="flex-1 flex items-center justify-center gap-2 rounded-xl bg-rose-500 px-4 py-3 text-sm font-semibold text-white hover:bg-rose-400 transition"
              >
                <div className="h-3 w-3 rounded-full bg-white animate-pulse" />
                Stop ({formatTime(30 - recordingTime)}s left)
              </button>
            )}
          </div>

          <p className="text-xs text-[var(--text-muted)] text-center">
            {step === "camera"
              ? "Press Start Recording and show yourself taking the medication. Max 30 seconds."
              : "Recording in progress... Show the medication clearly."}
          </p>
        </div>
      )}

      {step === "review" && videoUrl && (
        <div className="surface rounded-2xl p-6 space-y-4">
          <div className="relative aspect-video bg-black rounded-xl overflow-hidden">
            <video src={videoUrl} controls className="w-full h-full" />
          </div>

          <div className="flex gap-3">
            <button
              onClick={retake}
              className="flex items-center justify-center gap-2 rounded-xl border border-[var(--border-color)] px-4 py-3 text-sm font-medium transition hover:bg-[var(--bg-hover)]"
            >
              <RotateCcw className="h-4 w-4" />
              Retake
            </button>
            <button
              onClick={submitVerification}
              className="flex-1 flex items-center justify-center gap-2 rounded-xl bg-teal-500 px-4 py-3 text-sm font-semibold text-slate-950 hover:bg-teal-400 transition"
            >
              <CheckCircle className="h-4 w-4" />
              Submit Verification
            </button>
          </div>
        </div>
      )}

      {step === "submitting" && (
        <div className="surface rounded-2xl p-8 text-center">
          <div className="animate-spin h-8 w-8 border-2 border-teal-500 border-t-transparent rounded-full mx-auto" />
          <p className="mt-4 text-sm font-medium">Submitting verification...</p>
          <p className="mt-1 text-xs text-[var(--text-muted)]">
            Uploading video securely...
          </p>
        </div>
      )}

      {step === "success" && (
        <div className="surface rounded-2xl p-8 text-center">
          <CheckCircle className="h-12 w-12 text-emerald-400 mx-auto" />
          <p className="mt-4 text-lg font-semibold">Verification Submitted</p>
          <p className="mt-2 text-sm text-[var(--text-muted)]">
            Your caregiver and care team have been notified.
          </p>
          <div className="mt-6 flex gap-3 justify-center">
            <button
              onClick={() => {
                retake();
              }}
              className="rounded-xl border border-[var(--border-color)] px-6 py-2 text-sm font-medium transition hover:bg-[var(--bg-hover)]"
            >
              Verify Another
            </button>
            <button
              onClick={() => navigate("/my-care")}
              className="rounded-xl bg-teal-500 px-6 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400 transition"
            >
              Back to My Care
            </button>
          </div>
        </div>
      )}

      {step === "error" && (
        <div className="surface rounded-2xl p-8 text-center">
          <AlertCircle className="h-12 w-12 text-rose-400 mx-auto" />
          <p className="mt-4 text-lg font-semibold">Submission Failed</p>
          <p className="mt-2 text-sm text-[var(--text-muted)]">{error}</p>
          <button
            onClick={() => setStep("review")}
            className="mt-6 rounded-xl bg-teal-500 px-6 py-2 text-sm font-semibold text-slate-950 hover:bg-teal-400 transition"
          >
            Try Again
          </button>
        </div>
      )}
    </div>
  );
}
