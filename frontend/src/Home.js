import React, { useState } from 'react';
import Slider from "react-slick";
import './Home.css';

const Home = () => {
  const [curl, setCurl] = useState('');
  const [description, setDescription] = useState('');
  const [apiData, setApiData] = useState([]);
  const [error, setError] = useState(null);
  const [currentSlide, setCurrentSlide] = useState(1);
  const [loading, setLoading] = useState(false);
  const [charLimit] = useState(500);


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
    if (value.length <= charLimit) { // Validate against character limit
      setDescription(value);
    }
  };

  const handleSubmit = async () => {
    console.log('cURL:', curl);
    console.log('Description:', description);
    setLoading(true);

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

      // Convert the API response to a format usable in cards
      const formattedData = Object.keys(data).map((key, index) => ({
        id: index + 1,
        title: key.replace(/\"/g, ''), // Clean up the key by removing extra quotes
        description: JSON.stringify(JSON.parse(data[key]), null, 2), // Parse and prettify the JSON data
      }));
      console.log(formattedData);

      setApiData(formattedData);
      setError(null); // Clear any previous errors
    } catch (err) {
      setError(err.message);
      console.error("Error:", err);
    } finally {
      setLoading(false); // Set loading to false when the API request finishes
    }
  };

  const handleInputChange = (id, key, value) => {
    const updatedData = apiData.map((item) => {
      if (item.id === id) {
        const updatedDescription = { ...JSON.parse(item.description), [key]: value };
        return { ...item, description: JSON.stringify(updatedDescription, null, 2) };
      }
      return item;
    });
    setApiData(updatedData);
  };

  return (
    <div className="outer-container" data-name="Sanity Checker - an AI based tool">
      <div className="home-app-container">
        <div className="curl-section-left">
          <div className="input-container">
            <textarea
              className="curl-input"
              value={curl}
              onChange={handleCurlChange}
              placeholder="Enter cURL here"
            />
            <textarea
              className="description-input"
              value={description}
              onChange={handleDescriptionChange}
              placeholder="Please enter the description of API here for better response."
              maxLength={charLimit}
            />
            <button
              className="submit-curl-btn"
              onClick={handleSubmit}
              disabled={!curl.trim() || loading || !description.trim()} // Disable the button if loading or inputs are empty
            >
              {loading ? 'Submitting...' : 'Submit cURL and description'} {/* Display loader text */}
            </button>
          </div>
        </div>
        <div className='curl-section-right'>
          <div className="carousel-container">
            <Slider {...settings}>
              {apiData.map((formattedData) => {
                const parsedDescription = JSON.parse(formattedData.description);
                return (
                  <div key={formattedData.id} className="carousel-card">
                    <label>
                      <input type="checkbox" className='checkbox' />
                      <span className='label-checkbox'>Test Case {formattedData.id}</span>
                    </label>
                    <h2>{formattedData.title}</h2>
                    <ul className="description-list">
                      {Object.keys(parsedDescription).map((key) => (
                        <li key={key} className="description-item">
                          <label>
                            <input type="checkbox" className="description-checkbox" />
                            <span className="key-name">{key}:</span>
                            <input
                              type="text"
                              className="value-input"
                              value={parsedDescription[key]}
                              onChange={(e) => handleInputChange(formattedData.id, key, e.target.value)}
                            />
                          </label>
                        </li>
                      ))}
                    </ul>
                  </div>
                )
              })}
            </Slider>
          </div>
          <div className="slide-count">
            {apiData.length > 0 && (
              <>
                {`${currentSlide} / ${apiData.length}`}
                <div style={{ display: 'flex', justifyContent: 'center', marginTop: '10px' }}>
                  <button
                    className="additional-button"
                    onClick={() => console.log('Button Clicked!')}
                  >
                    Click Me
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;
