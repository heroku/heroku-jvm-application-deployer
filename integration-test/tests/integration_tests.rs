// Required due to: https://github.com/rust-lang/rust/issues/95513
#![allow(unused_crate_dependencies)]

use integration_test::*;
use std::process::Command;

#[test]
fn basic_war_app() {
    prepare_test("war-app", None, |context| {
        create_empty_git_commit(&context.app_dir);

        run_command(
            Command::new("java")
                .args([
                    "-jar",
                    &heroku_jvm_application_deployer_jar_path().to_string_lossy(),
                    &context
                        .app_dir
                        .join("target/test-1.0-SNAPSHOT.war")
                        .to_string_lossy(),
                    "--jdk",
                    "21",
                ])
                .current_dir(&context.app_dir),
            "Running heroku-jvm-application-deployer failed.",
            false,
        );

        let response = http_get_expect_200(&context.app_url);
        assert_eq!(response, String::from("Hello World!"));
    });
}

#[test]
fn basic_jar_app() {
    prepare_test("jar-app", None, |context| {
        create_empty_git_commit(&context.app_dir);

        run_command(
            Command::new("java")
                .args([
                    "-jar",
                    &heroku_jvm_application_deployer_jar_path().to_string_lossy(),
                    &context
                        .app_dir
                        .join("target/test-1.0-SNAPSHOT-jar-with-dependencies.jar")
                        .to_string_lossy(),
                    "--jdk",
                    "21",
                ])
                .current_dir(&context.app_dir),
            "Running heroku-jvm-application-deployer failed.",
            false,
        );

        let response = http_get_expect_200(&context.app_url);
        assert_eq!(response, String::from("Hello World!"));
    });
}

#[test]
fn basic_jar_app_git_repo_without_commits() {
    prepare_test("jar-app", None, |context| {
        // Note that no git commit is created

        run_command(
            Command::new("java")
                .args([
                    "-jar",
                    &heroku_jvm_application_deployer_jar_path().to_string_lossy(),
                    &context
                        .app_dir
                        .join("target/test-1.0-SNAPSHOT-jar-with-dependencies.jar")
                        .to_string_lossy(),
                    "--jdk",
                    "21",
                ])
                .current_dir(&context.app_dir),
            "Running heroku-jvm-application-deployer failed.",
            false,
        );

        let response = http_get_expect_200(&context.app_url);
        assert_eq!(response, String::from("Hello World!"));
    });
}

#[test]
#[ignore = "General Fir CI testing strategy for tools is pending"]
fn basic_war_app_fir() {
    prepare_test("war-app", Some("heroku-languages-jvm-ci"), |context| {
        // Note that no project.toml exists in the test fixture and that it will be generated by
        // the JVM application deployer automatically.
        assert!(!context.app_dir.join("project.toml").exists());

        create_empty_git_commit(&context.app_dir);

        let output = run_command(
            Command::new("java")
                .args([
                    "-jar",
                    &heroku_jvm_application_deployer_jar_path().to_string_lossy(),
                    &context
                        .app_dir
                        .join("target/test-1.0-SNAPSHOT.war")
                        .to_string_lossy(),
                    "--jdk",
                    "21",
                ])
                .current_dir(&context.app_dir),
            "Running heroku-jvm-application-deployer failed.",
            false,
        );

        assert!(
            String::from_utf8_lossy(&output.stdout).contains("- including: project.toml (hidden)")
        );

        let response = http_get_expect_200(&context.app_url);
        assert_eq!(response, String::from("Hello World!"));
    });
}
