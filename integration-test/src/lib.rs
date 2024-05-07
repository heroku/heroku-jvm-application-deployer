use fs_extra::dir::CopyOptions;
use libherokubuildpack::command::CommandExt;
use serde::Deserialize;
use std::io::{stderr, stdout};
use std::path::{Path, PathBuf};
use std::process::{Command, Output};
use std::time::Duration;

pub fn prepare_test<F>(fixture_dir: &str, f: F)
where
    F: Fn(TestContext),
{
    let app_dir = prepare_fixture(fixture_dir);
    initialize_git_repository(&app_dir);
    let app_create_result = create_heroku_app(&app_dir);

    f(TestContext {
        app_name: app_create_result.name,
        app_dir,
        app_url: app_create_result.web_url,
    });
}

pub struct TestContext {
    pub app_name: String,
    pub app_dir: PathBuf,
    pub app_url: String,
}

impl Drop for TestContext {
    fn drop(&mut self) {
        destroy_heroku_app(&self.app_name);
    }
}

#[must_use]
pub fn heroku_jvm_application_deployer_jar_path() -> PathBuf {
    let maven_target_dir = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("..")
        .join("target");

    for dir_entry in std::fs::read_dir(maven_target_dir).unwrap() {
        let dir_entry = dir_entry.unwrap();
        let file_name = dir_entry.file_name().to_string_lossy().into_owned();

        if file_name.starts_with("heroku-jvm-application-deployer") && file_name.ends_with(".jar") {
            return dir_entry.path();
        }
    }

    panic!("Could not find heroku-jvm-application-deployer JAR file!");
}

#[must_use]
pub fn http_get_expect_200(url: &str) -> String {
    thread_sleep_backoff(10, Duration::from_secs(1), Duration::from_secs(30), || {
        ureq::get(url).call()
    })
    .expect("")
    .into_string()
    .expect("")
}

#[must_use]
pub fn create_heroku_app(path: &Path) -> HerokuAppCreateResult {
    let output = run_command(
        Command::new("heroku")
            .args(["create", "--json"])
            .current_dir(path),
        &format!("Could not create Heroku app in {path:?}"),
        true,
    );

    serde_json::from_slice::<HerokuAppCreateResult>(&output.stdout).unwrap()
}

#[derive(Deserialize)]
pub struct HerokuAppCreateResult {
    pub name: String,
    pub web_url: String,
}

pub fn destroy_heroku_app(app_name: &str) {
    run_command(
        Command::new("heroku").args(["destroy", app_name, "--confirm", app_name]),
        &format!("Could not destroy Heroku app {app_name}"),
        true,
    );
}

pub fn initialize_git_repository(path: &Path) {
    run_command(
        Command::new("git").args(["init"]).current_dir(path),
        &format!("Could not initialize git repository in {path:?}"),
        true,
    );
}

pub fn create_empty_git_commit(path: &Path) {
    run_command(
        Command::new("git")
            .args(["commit", "--allow-empty", "-m", "empty"])
            .current_dir(path),
        &format!("Could create empty git commit in {path:?}"),
        true,
    );
}

pub fn run_command(command: &mut Command, error: &str, suppress_output: bool) -> Output {
    let output = if suppress_output {
        command.output()
    } else {
        command.output_and_write_streams(stdout(), stderr())
    }
    .expect("");

    assert!(
        output.status.success(),
        "{error} exit code: {}",
        output
            .status
            .code()
            .map_or(String::from("<unknown>"), |exit_code| format!(
                "{exit_code}"
            ))
    );

    output
}

#[must_use]
pub fn prepare_fixture(name: &str) -> PathBuf {
    let temp_dir = tempfile::tempdir().expect("").into_path();

    let fixture_path = PathBuf::from(name);

    fs_extra::copy_items(
        &[PathBuf::from("fixtures").join(&fixture_path)],
        &temp_dir,
        &CopyOptions {
            copy_inside: false,
            ..Default::default()
        },
    )
    .expect("");

    temp_dir.join(&fixture_path)
}

fn thread_sleep_backoff<F, T, E>(
    retries: u32,
    min: Duration,
    max: Duration,
    request_fn: F,
) -> Result<T, E>
where
    F: Fn() -> Result<T, E>,
{
    let backoff = exponential_backoff::Backoff::new(retries, min, max);

    let mut backoff_durations = backoff.into_iter();

    loop {
        match request_fn() {
            result @ Ok(_) => return result,
            result @ Err(_) => match backoff_durations.next() {
                None => return result,
                Some(backoff_duration) => {
                    std::thread::sleep(backoff_duration);
                    continue;
                }
            },
        }
    }
}
