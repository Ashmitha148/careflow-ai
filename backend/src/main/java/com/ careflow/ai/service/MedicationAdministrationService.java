public MedicationAdministrationService(
            MedicationAdministrationRepository administrationRepository,
            MedicationRepository medicationRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            PatientAccessRepository patientAccessRepository,
            TimelineService timelineService,
            AuditLogService auditLogService) {
        this.administrationRepository = administrationRepository;
        this.medicationRepository = medicationRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.patientAccessRepository = patientAccessRepository;
        this.timelineService = timelineService;
        this.auditLogService = auditLogService;