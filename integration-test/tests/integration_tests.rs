use integration_test::*;
use std::process::Command;

#[test]
fn basic_war_app() {
    prepare_test("war-app", |context| {
        println!(">> {:?}", heroku_deploy_standalone_path());

        run_command(
            Command::new("java")
                .args([
                    "-jar",
                    &heroku_deploy_standalone_path().to_string_lossy(),
                    &context
                        .app_dir
                        .join("target/test-1.0-SNAPSHOT.war")
                        .to_string_lossy(),
                    "--jdk",
                    "21",
                ])
                .current_dir(&context.app_dir),
            "Running heroku-deploy-standalone failed.",
            false,
        );

        let response = http_get_expect_200(&context.app_url);
        assert_eq!(response, String::from("Hello World!"));
    });
}

#[test]
fn basic_jar_app() {
    prepare_test("jar-app", |context| {
        run_command(
            Command::new("java")
                .args([
                    "-jar",
                    &heroku_deploy_standalone_path().to_string_lossy(),
                    &context
                        .app_dir
                        .join("target/test-1.0-SNAPSHOT-jar-with-dependencies.jar")
                        .to_string_lossy(),
                    "--jdk",
                    "21",
                ])
                .current_dir(&context.app_dir),
            "Running heroku-deploy-standalone failed.",
            false,
        );

        let response = http_get_expect_200(&context.app_url);
        assert_eq!(response, String::from("Hello World!"));
    });
}
