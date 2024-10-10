import React, { useState } from 'react';
import Slider from "react-slick";
import './Home.css';
import { createToast } from 'react-simple-toasts';
import 'react-simple-toasts/dist/style.css';
import JSONPretty from 'react-json-pretty';


const Home = () => {
  const [curl, setCurl] = useState('');
  const [description, setDescription] = useState('');
  const [apiData, setApiData] = useState([]);
  const [error, setError] = useState(null);
  const [currentSlide, setCurrentSlide] = useState(1);
  const [loadingCurl, setLoadingCurl] = useState(false);
  const [loading, setLoading] = useState(false);
  const [charLimit] = useState(500);
  const [toastMessage, setToastMessage] = useState('');
  const [formattedResponse, setFormattedResponse] = useState([]);

  const settings = {
    dots: false,
    infinite: true,
    speed: 500,
    slidesToShow: 1,
    slidesToScroll: 1,
    arrows: true,
    arrows: apiData.length > 0 && !loading,
    afterChange: (current) => setCurrentSlide(current + 1),
  };


  const handleCurlChange = (e) => {
    setCurl(e.target.value);
  };

  const handleDescriptionChange = (e) => {
    const value = e.target.value;
    if (value.length <= charLimit) {
      setDescription(value);
    }
  };

  const handleSubmit = async () => {
    setLoadingCurl(true); // Set curl loading to true

    try {
      const requestBody = JSON.stringify({
        curl: curl,
        description: description
      });

      const response = await fetch('http://localhost:8090/requestFromCurl', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: requestBody
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        const errorMessage = error.message || 'Unable to parse the cURL';
        throw new Error(errorMessage);
      }

      const data = await response.json();
      const formattedData = Array.isArray(data) ? data.map((item, index) => ({
        id: index + 1,
        title: item.testCaseName || `Test Case ${index + 1}`,
        description: item.testRequestBody,
        validJSON: item.validJSON,
        isSelected: true
      })) : [{
        id: 1,
        title: data.testCaseName || 'Test Case 1',
        description: data.testRequestBody,
        validJSON: data.validJSON,
        isSelected: true
      }];

      setApiData(formattedData);
      setError(null);
    } catch (err) {
      setError(err.message);
      console.error("Error:", err);
    } finally {
      setLoadingCurl(false); // Set curl loading to false
    }
  };

  const handleInputChange = (id, value) => {
    const updatedData = apiData.map((item) => {
      if (item.id === id) {
        return { ...item, description: value };
      }
      return item;
    });
    setApiData(updatedData);
  };

  const handleCheckboxChange = (id) => {
    const updatedData = apiData.map((item) => {
      if (item.id === id) {
        return { ...item, isSelected: !item.isSelected };
      }
      return item;
    });
    setApiData(updatedData);
  };

  const handleErrorToast = (errorMessage) => {
    createToast(errorMessage, {
      type: 'error',
    });
  };

  const handleRunTestCase = async () => {
    const selectedTestCases = apiData.filter(item => item.isSelected);

    if (selectedTestCases.length === 0) {
      console.error("No test cases selected");
      return;
    }

    setFormattedResponse([]);

    setLoading(true); // Set loading for test cases

    try {
      const requestBody = {
        curl: curl,
        requestBodyList: selectedTestCases.map(item => ({
          tcId: item.id.toString(),
          testRequestBody: item.description
        }))
      };

      const response = await fetch('http://localhost:8090/runTestCase', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        const errorMessage = error.message || 'Failed to run test case';
        throw new Error(errorMessage);
      }

      const data = await response.json();
      const formattedResponse = data.map(item => {
        const tcResponse = JSON.parse(item.tcResponse);
        return {
          tcId: item.tcId,
          statusCode: item.statusCode,
          message: tcResponse.message,
          validationErrors: tcResponse.validationErrors,
        };
      });
      setFormattedResponse(formattedResponse);
      console.log('Test case run successfully:', formattedResponse);
    } catch (err) {
      console.error("Error running test case:", err.message);
      setToastMessage("Error running test case");
      handleErrorToast(err.message);
    } finally {
      setLoading(false); // Set loading for test cases to false
    }
  };

  return (
    <div className="outer-container">
      <div className="header">
        <h1 className='home-home-header'>AI-Powered Sanity Checker</h1>
      </div>
      <div className="home-app-container">
        <div className="curl-section-left">
          <div className="input-container">
            <textarea
              className="curl-input"
              value={curl}
              onChange={handleCurlChange}
              placeholder="Enter cURL here"
              spellCheck={false}
            />
            <textarea
              className="description-input"
              value={description}
              onChange={handleDescriptionChange}
              placeholder="Please enter the description of API here for better response."
              maxLength={charLimit}
              spellCheck={false}
            />
            <button
              className="submit-curl-btn"
              onClick={handleSubmit}
              disabled={!curl.trim() || loadingCurl || !description.trim()}
            >
              {loadingCurl ? 'Submitting...' : 'Submit cURL and description'}
            </button>
          </div>
        </div>
        <div className='curl-section-right'>
          <div className="carousel-container">
            <Slider {...settings}>
              {apiData.map((formattedData) => (
                <div key={formattedData.id} className="carousel-card">
                  <label>
                    <input
                      type="checkbox"
                      className='checkbox'
                      checked={formattedData.isSelected}
                      onChange={() => handleCheckboxChange(formattedData.id)}
                    />
                    <span className='label-checkbox'>Test Case {formattedData.id}</span>
                  </label>
                  <h2 className='home-card-heading'>{formattedData.title}</h2>
                  <textarea
                    className="json-editor"
                    value={formattedData.description}
                    onChange={(e) => handleInputChange(formattedData.id, e.target.value)}
                    rows={10}
                    spellCheck="false"
                  />
                </div>
              ))}
            </Slider>
          </div>
          <div className="slide-count">
            {apiData.length > 0 && (
              <>
                {`${currentSlide} / ${apiData.length}`}
                <div className='home-Bottom-container'>
                  <button
                    className="additional-button"
                    onClick={handleRunTestCase}
                    disabled={loading}
                  >
                    {loading ? 'Running...' : 'Submit Requests'}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
      <hr className="divider" />
      <div>
        {formattedResponse.length > 0 && (
          <div className='bottom-container'>
            <h2 className="bottom-header">Test Case Responses</h2>
            {/* <JSONPretty data={formattedResponse}></JSONPretty> */}
            <div className="response-carousel-container">
              <Slider {...settings}>
                {formattedResponse.map((item) => (
                  <div key={item.tcId} className="response-item-card">
                    <h3 className='label-checkbox'>Test Case: {item.tcId}</h3>
                    <p className='status-code'>Status Code: {item.statusCode}</p>
                    <p className='response-message'>Message: {item.message}</p>
                    {/* {item.validationErrors && item.validationErrors.length > 0 && (
                      <ul>
                        {item.validationErrors.map((error, index) => (
                          <li key={index} className='validation-error'>
                            {error.field}: {error.message}
                          </li>
                        ))}
                      </ul>
                    )} */}
                  </div>
                ))}
              </Slider>
            </div>
          </div>
        )}
      </div>
      {formattedResponse.some(item => item.validationErrors && item.validationErrors.length > 0) &&
        handleErrorToast("Error running test case")
      }
    </div>
  );
};

export default Home;
